package com.example.attendancesystem.view.fragments

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendancesystem.*
import com.example.attendancesystem.api.factory.*
import com.example.attendancesystem.api.listener.*
import com.example.attendancesystem.api.repository.*
import com.example.attendancesystem.databinding.FragmentAttendanceBinding
import com.example.attendancesystem.models.*
import com.example.attendancesystem.utils.*
import com.example.attendancesystem.utils.Constants.ExtraKey.IMAGE_CAPTURE_INTENT
import com.example.attendancesystem.view.adapters.*
import com.example.attendancesystem.view.dialogs.StudentDetailsDialog
import com.example.attendancesystem.view.listeners.OnItemClickListener
import com.example.attendancesystem.viewmodel.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class AttendanceFragment : Fragment() {
    
    private lateinit var binding: FragmentAttendanceBinding
    
    //VIEW MODEL
    private lateinit var uploadStudentImages: InferViewModel
    private lateinit var submitStudentListViewModel: SubmitStudentListViewModel
    
    //ADAPTER
    private lateinit var upLoadListAdapter: UpLoadListAdapter
    private lateinit var presentStudentAdapter: PresentStudentAdapter
    
    //ARRAY LIST
    private var arrayUploadFiles: ArrayList<Attachments> = arrayListOf()
    private var arrayPresentStudent: ArrayList<StudentDetailsItem> = arrayListOf()
    
    //VARIABLE LIST
    private var bottomSheet = BottomSheetDialogFragment()
    private var photoFile: File? = null
    private var isClick: Boolean = false
    private val launcherMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uriList ->
        requireActivity().setAttachment(uriList)
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAttendanceBinding.inflate(inflater)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        onClicks()
        setAdapter()
        setObserver()
    }
    
    private fun init() {
        uploadStudentImages = ViewModelProvider(this, InferViewModelFactory(InferRepository(requireActivity().request(InferApiRequest::class.java))))[InferViewModel::class.java]
        submitStudentListViewModel = ViewModelProvider(this, SubmitStudentListViewModelFactory(SubmitStudentListRepository(requireActivity().request(SubmitStudentListApiRequest::class.java))))[SubmitStudentListViewModel::class.java]
    }
    
    private fun onClicks() {
        binding.apply {
            llUploadPhotoOrVideo.setOnClickListener {
                if (!isClick) {
                    isClick = true
                    setDialog()
                }
            }
            
            rvPresentStudentList
            
            btnAddStudentInformation.setOnClickListener {
                if (arrayUploadFiles.size > 0) {
                    try {
                        bottomSheet = StudentDetailsDialog(object : OnItemClickListener {
                            override fun onItemClick(value: Any?) {
                                super.onItemClick(value)
                                value as StudentInformation
                                Log.d("TAG", "onItemClick: ${Gson().toJson(value)} ")
                                
                                viewGone(ivMissingData)
                                arrayPresentStudent.clear()
                                val jsonStudentInfo = App.gson.toJson(value)
                                val studentInfoPart = jsonStudentInfo.toRequestBody("text/plain".toMediaType())
                                
                                viewVisible(progressBar)
                                uploadStudentImages.apiCallForUploadOInferAttachment(arrayUploadFiles, studentInfoPart)
                                bottomSheet.dismiss()
                            }
                        })
                        bottomSheet.show(requireActivity().supportFragmentManager, bottomSheet.tag)
                    } catch (e: Exception) {
                        requireActivity().toast(e.message)
                    }
                    
                } else {
                    requireActivity().toast("Please upload the images")
                }
            }
            
            btnSubmitStudentDetails.setOnClickListener {
                submitStudentListViewModel.apiCallForSubmitStudentList(arrayPresentStudent)
            }
        }
    }
    
    private fun setDialog() {
        val alertDialog = AlertDialog.Builder(requireActivity())
        
        alertDialog.setOnDismissListener {
            isClick = false
        }
        val item = mutableListOf(getString(R.string.camera), getString(R.string.gallery))
        
        alertDialog.setItems(item.toTypedArray()) { dialog, index ->
            when (item[index]) {
                getString(R.string.gallery) -> {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                        addFlags(takeFlags)
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        type = getString(R.string.image_type)
                    }
                    
                    launcherMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    dialog.dismiss()
                }
                
                getString(R.string.camera) -> {
                    val cameraIntent = Intent(IMAGE_CAPTURE_INTENT)
                    cameraIntent.resolveActivity(requireActivity().packageManager)?.let {
                        photoFile = requireActivity().createImageFile()
                        val photoURI: Uri? = photoFile?.let { photoFile ->
                            FileProvider.getUriForFile(requireActivity(), "${requireActivity().packageName}.provider", photoFile)
                        }
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        captureImageRequestContract.launch(cameraIntent)
                    }
                    dialog.dismiss()
                }
            }
        }
        val dialog = alertDialog.create()
        dialog.show()
    }
    
    private fun setAdapter() {
        binding.apply {
            upLoadListAdapter = UpLoadListAdapter(arrayUploadFiles, object : OnItemClickListener {
                override fun onItemClick(value: Any?) {
                    super.onItemClick(value)
                    value as Attachments
                    
                    if (value.awsImageUrl?.isNotEmpty() == true) {
                        ShowAttachmentDialogFragment.newInstance(value.awsImageUrl, value.type, value.key).apply {
                            show(requireActivity().supportFragmentManager, ShowAttachmentDialogFragment.TAG)
                        }
                        
                    } else if (value.path?.isNotEmpty() == true) {
                        ShowAttachmentDialogFragment.newInstance(value.path, value.type, value.key).apply {
                            show(requireActivity().supportFragmentManager, ShowAttachmentDialogFragment.TAG)
                        }
                        
                    }
                }
                
                override fun onItemClick(key: Any?, value: Any?) {
                    super.onItemClick(key, value)
                    key as Int
                    value as Attachments
                    if (key > -1) {
                        arrayUploadFiles.removeAt(key)
                        upLoadListAdapter.notifyItemRemoved(key)
                    }
                }
            })
            
            rvAttachments.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
            rvAttachments.adapter = upLoadListAdapter
            
            presentStudentAdapter = PresentStudentAdapter(arrayPresentStudent, object : OnItemClickListener {
                override fun onItemClick(key: Any?, value: Any?) {
                    key as Int
                    value as StudentDetailsItem
                    super.onItemClick(value)
                    requireActivity().toast(value.name)
                    if (key > -1) {
                        arrayPresentStudent[key].status = !arrayPresentStudent[key].status
                        presentStudentAdapter.notifyItemChanged(key)
                    }
                }
                
            })
            rvPresentStudentList.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
            rvPresentStudentList.adapter = presentStudentAdapter
        }
    }
    
    @SuppressLint("NotifyDataSetChanged")
    private val captureImageRequestContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val filePath = photoFile?.path
            val imageBitMap = BitmapFactory.decodeFile(filePath)
            val resizedBitMap = Bitmap.createScaledBitmap(imageBitMap, imageBitMap.width / 3, imageBitMap.height / 3, true)
            val resizedFile = requireActivity().bitmapToFile(resizedBitMap)
            val resizeImagePath = Uri.fromFile(resizedFile).toString()
            val upLoadImageOrVideo = Attachments().apply {
                this.path = Uri.parse(resizeImagePath).toString()
                type = getString(R.string.image)
            }
            arrayUploadFiles.add(upLoadImageOrVideo)
            upLoadListAdapter.notifyDataSetChanged()
            
        } else {
            photoFile = null
        }
    }
    
    @SuppressLint("NotifyDataSetChanged")
    private fun Activity.setAttachment(uriList: List<Uri>?) {
        try {
            uriList?.let { list ->
                list.forEach { imgUri ->
                    imgUri.let { uri ->
                        val path = getFilePathFromUri(this, uri, true)
                        if (path.toString().contains(getString(R.string.jpg)) || path.toString().contains(getString(R.string.jpeg)) || path.toString().contains(getString(R.string.png)) || path.toString().contains(getString(R.string.heic)) || path.toString().contains(getString(R.string.gif))) {
                            val photoFile = File(path.toString())
                            val filePath = photoFile.path
                            val imageBitMap = BitmapFactory.decodeFile(filePath)
                            val resizedFile = bitmapToFile(imageBitMap)
                            val resizeImagePath = Uri.fromFile(resizedFile).toString()
                            val upLoadImageOrVideo = Attachments().apply {
                                this.path = Uri.parse(resizeImagePath).toString()
                                type = getString(R.string.jpg)
                                isImageLoaded = true
                            }
                            arrayUploadFiles.add(upLoadImageOrVideo)
                            upLoadListAdapter.notifyDataSetChanged()
                            
                        } else {
                            toast(getString(R.string.error_invalid_photo_video_type))
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            e.message
        }
    }
    
    @SuppressLint("NotifyDataSetChanged")
    private fun setObserver() {
        binding.apply {
            uploadStudentImages.getUploadInferAttachmentApiResponse.observe(requireActivity()) { apiResponse ->
                viewGone(progressBar)
                apiResponse?.data?.let { data ->
                    val serviceCategoryData = getArrayData(data, object : TypeToken<java.util.ArrayList<StudentDetailsItem>>() {})
                    
                    serviceCategoryData?.forEach { item ->
                        arrayPresentStudent.add(item)
                    }
                    if (arrayPresentStudent.isEmpty()) {
                        viewVisible(ivMissingData)
                        viewGone(binding.llStudentList)
                        
                    } else {
                        viewVisible(binding.llStudentList)
                        viewGone(ivMissingData)
                        presentStudentAdapter.notifyDataSetChanged()
                    }
                }
            }
            
            submitStudentListViewModel.getSubmitStudentListApiResponse.observe(requireActivity()) { apiResponse ->
                viewGone(progressBar)
                Log.d("TAG", "setObserver: ${Gson().toJson(apiResponse)} ")
            }
        }
    }
}