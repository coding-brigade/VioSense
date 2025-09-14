import sys
import os
import torch
import torch.nn as nn
import torchvision.models as models
from torch.nn.parameter import Parameter
import torch.nn.functional as F

# Add the root directory of the project to the sys.path
sys.path.append(
    os.path.abspath(os.path.join(os.path.dirname(__file__), "../../../../"))
)


class ChannelAttention(nn.Module):
    def __init__(self, in_channels, reduction=16):
        super(ChannelAttention, self).__init__()
        self.avg_pool = nn.AdaptiveAvgPool2d(1)  # Global Average Pooling
        self.max_pool = nn.AdaptiveMaxPool2d(1)  # Global Max Pooling

        # shared MLP
        self.mlp = nn.Sequential(
            nn.Conv2d(in_channels, in_channels // reduction, bias=False, kernel_size=1),
            nn.ReLU(inplace=True),
            nn.Conv2d(in_channels // reduction, in_channels, bias=False, kernel_size=1),
        )

        self.sigmoid = nn.Sigmoid()

    def forward(self, x):
        avg_out = self.mlp(self.avg_pool(x))
        max_out = self.mlp(self.max_pool(x))
        attention = self.sigmoid(avg_out + max_out)  # addition then sigmoid
        return x * attention  # Multiply with input feature map


class SpatialAttention(nn.Module):
    def __init__(self, kernel_size=7):
        super(SpatialAttention, self).__init__()
        self.conv = nn.Conv2d(2, 1, kernel_size, padding=kernel_size // 2, bias=False)
        self.sigmoid = nn.Sigmoid()

    def forward(self, x):
        avg_out = torch.mean(x, dim=1, keepdim=True)  # Average pooling across channels
        max_out, _ = torch.max(x, dim=1, keepdim=True)  # Max pooling across channels
        attention = self.conv(
            torch.cat([avg_out, max_out], dim=1)
        )  # Concatenate along channel dim
        return x * self.sigmoid(attention)  # Multiply with input feature map


class PatchActivation(nn.Module):
    def __init__(self, channels, num_grids=3, groups=64):
        super(PatchActivation, self).__init__()

        self.num_grids = num_grids

        # Ensure groups is a valid value
        if groups == 0 or channels % (2 * groups) != 0:
            groups = 1

        # Grid processing components
        self.conv = nn.Conv2d(channels, channels, kernel_size=3, padding=1)
        self.bn = nn.BatchNorm2d(channels)
        self.adaptive_pool = nn.AdaptiveAvgPool2d((1, 1))
        self.relu = nn.ReLU(inplace=True)

        # Shuffle Attention
        self.shuffle_attention = ShuffleAttention(channels, groups)
        self.sigmoid = nn.Sigmoid()

    def process_grid(self, grid):
        # Step 2: Process individual grid
        identity = grid

        # Apply Shuffle Attention
        out = self.shuffle_attention(grid)

        # Apply Batch Normalization
        out = self.bn(out)

        # Apply Adaptive Pooling
        out = self.adaptive_pool(out)

        # Apply Sigmoid
        out = self.sigmoid(out)

        # Expand pooled features to match grid size
        out = out.expand_as(identity)

        return out  # Do not add identity here to allow for proper attention weighting

    def forward(self, x):
        B, C, H, W = x.shape

        # Step 1: Ensure dimensions are divisible by num_grids with padding
        pad_h = (self.num_grids - H % self.num_grids) % self.num_grids
        pad_w = (self.num_grids - W % self.num_grids) % self.num_grids
        if pad_h > 0 or pad_w > 0:
            x = F.pad(x, (0, pad_w, 0, pad_h))

        # Calculate grid size
        grid_h = (H + pad_h) // self.num_grids
        grid_w = (W + pad_w) // self.num_grids

        # Divide into grids and process each
        output_grids = []
        for i in range(self.num_grids):
            for j in range(self.num_grids):
                # Extract grid
                grid = x[
                    :, :, i * grid_h : (i + 1) * grid_h, j * grid_w : (j + 1) * grid_w
                ]

                # Process grid
                processed_grid = self.process_grid(grid)
                output_grids.append(processed_grid)

        # Step 3: Reconstruct the full feature map
        rows = []
        for i in range(self.num_grids):
            row = torch.cat(
                output_grids[i * self.num_grids : (i + 1) * self.num_grids], dim=3
            )
            rows.append(row)
        output = torch.cat(rows, dim=2)

        # Remove padding if added
        if pad_h > 0 or pad_w > 0:
            output = output[:, :, :H, :W]

        # Multiply the input feature map with the attention feature map
        attention = output * x[:, :, :H, :W]  # Ensure matching dimensions
        return attention


class UPSCA(nn.Module):
    def __init__(
        self, in_channels, reduction=16, spatial_kernel=7, num_grids=3, groups=64
    ):
        super(UPSCA, self).__init__()
        self.channel_attention = ChannelAttention(in_channels, reduction)
        self.patch_activation = PatchActivation(in_channels, num_grids, groups)
        self.spatial_attention = SpatialAttention(spatial_kernel)

    def forward(self, x):
        channel_out = self.channel_attention(x)  # Apply Channel Attention
        patch_out = self.patch_activation(channel_out)  # Apply Patch Activation
        spatial_out = self.spatial_attention(patch_out)  # Apply Spatial Attention
        return (
            spatial_out + x
        )  # Add the original input to the attention weighted feature map


class ShuffleAttention(nn.Module):
    """Constructs a Channel Spatial Group module.

    Args:
        channel: Number of input channels
        groups: Number of groups for channel shuffle
    """

    def __init__(self, channel, groups=64):
        super(ShuffleAttention, self).__init__()
        self.groups = groups
        self.avg_pool = nn.AdaptiveAvgPool2d(1)
        self.cweight = Parameter(torch.zeros(1, channel // (2 * groups), 1, 1))
        self.cbias = Parameter(torch.ones(1, channel // (2 * groups), 1, 1))
        self.sweight = Parameter(torch.zeros(1, channel // (2 * groups), 1, 1))
        self.sbias = Parameter(torch.ones(1, channel // (2 * groups), 1, 1))

        self.sigmoid = nn.Sigmoid()
        self.gn = nn.GroupNorm(channel // (2 * groups), channel // (2 * groups))

    @staticmethod
    def channel_shuffle(x, groups):
        b, c, h, w = x.shape

        x = x.reshape(b, groups, -1, h, w)
        x = x.permute(0, 2, 1, 3, 4)

        # flatten
        x = x.reshape(b, -1, h, w)

        return x

    def forward(self, x):
        b, c, h, w = x.shape

        x = x.reshape(b * self.groups, -1, h, w)
        x_0, x_1 = x.chunk(2, dim=1)

        # channel attention
        xn = self.avg_pool(x_0)
        xn = self.cweight * xn + self.cbias
        xn = x_0 * self.sigmoid(xn)

        # spatial attention
        xs = self.gn(x_1)
        xs = self.sweight * xs + self.sbias
        xs = x_1 * self.sigmoid(xs)

        # concatenate along channel axis
        out = torch.cat([xn, xs], dim=1)
        out = out.reshape(b, -1, h, w)

        out = self.channel_shuffle(out, 2)
        return out


class ResNet18WithUPSCA(nn.Module):
    def __init__(self, num_classes=1000):
        super(ResNet18WithUPSCA, self).__init__()
        self.resnet18 = models.resnet18(pretrained=True)

        # Replace the basic blocks with UPSCA blocks
        self.resnet18.layer1 = self._make_layer(self.resnet18.layer1)
        self.resnet18.layer4 = self._make_layer(self.resnet18.layer4)

        # Modify the final fully connected layer
        self.resnet18.fc = nn.Linear(self.resnet18.fc.in_features, num_classes)

    def _make_layer(self, layer):
        layers = []
        for block in layer:
            layers.append(nn.Sequential(block, UPSCA(block.conv1.out_channels)))
        return nn.Sequential(*layers)

    def forward(self, x):
        return self.resnet18(x)


def get_model(num_classes=1000):
    model = ResNet18WithUPSCA(num_classes=num_classes)
    print(model)
    return model


# Example usage:
if __name__ == "__main__":
    model = get_model(num_classes=10)
    print(model)
