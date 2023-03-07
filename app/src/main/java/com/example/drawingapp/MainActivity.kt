package com.example.drawingapp

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.security.ProtectionDomain
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {
    private var drawingView : DrawingView? = null
    private var buttonBrush : ImageButton? = null
    private var customProgressDialog : Dialog
    ? = null


    //same as start Activity for result in java
    val openGallery : ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if(result.resultCode == RESULT_OK && result.data != null){
                //URI is the path of the item stored
                val backgroundImage : ImageView = findViewById(R.id.iv_background)
                backgroundImage.setImageURI(result.data?.data)
            }
        }

    val requestPermission : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        {
            permission ->
            permission.entries.forEach {
                val name = it.key
                val isGranted = it.value

                if(isGranted){
                    Toast.makeText(this,"Permission granted",Toast.LENGTH_LONG).show()

                    //open gallery with the help of intent
                    val pickIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                    openGallery.launch(pickIntent)
                }
                else{
                    if(name == android.Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(this,"OOPS!! Permission to external storage not granted!",Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    private var mImageButtonCurrentPaint : ImageButton? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.draw_view)
        drawingView?.setSizeForBrush(20.toFloat())

        val linearLayoutColors = findViewById<LinearLayout>(R.id.ll_paint_colors)
        mImageButtonCurrentPaint = linearLayoutColors[0] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
        )


        buttonBrush = findViewById(R.id.id_brush)
        buttonBrush?.setOnClickListener{
            showBrushSizeChooserDialog()
        }

        val ButtonGallery : ImageButton = findViewById(R.id.id_gallery)
        ButtonGallery.setOnClickListener{
            requestStoragePermission()
        }

        val undoButton : ImageButton = findViewById(R.id.undo)
        undoButton.setOnClickListener{
                drawingView?.onClickUndo()
        }

        val redoButton : ImageButton = findViewById(R.id.redo)
        redoButton.setOnClickListener{
            drawingView?.onClickRedo()
        }

        val save : ImageButton = findViewById(R.id.id_save)
        save.setOnClickListener{
            if(isReadStorageAllowed()){
                showProgressDialog()
                lifecycleScope.launch {
                    val drawingView : FrameLayout = findViewById(R.id.fl_drawing_view_container)
                  saveBitmapFile(getBitmapFromView(drawingView))
                }
            }
        }

    }

    private fun getBitmapFromView(view : View) : Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)

        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)
        return returnedBitmap

    }
    private suspend fun saveBitmapFile(bitmap: Bitmap?) : String{
        var result = ""
        withContext(Dispatchers.IO){
            if(bitmap != null){
                try {
                    val bytes = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)

                    val file  = File(externalCacheDir?.absoluteFile.toString() + File.separator + getString(R.string.app_name) + System.currentTimeMillis()/1000+" .png")

                    val fo = FileOutputStream(file)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = file.absolutePath

                    runOnUiThread{
                        cancelProgressDialog()
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,"file saved successfully at $result",Toast.LENGTH_LONG).show()
                            shareImage(result)
                        }else{
                            Toast.makeText(this@MainActivity,"Something went wrong while saving the file",Toast.LENGTH_LONG).show()
                        }

                    }
                }
                catch(e : Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return  result
    }
    private fun isReadStorageAllowed() : Boolean
    {
        val result = ContextCompat.checkSelfPermission(this,
        android.Manifest.permission.READ_EXTERNAL_STORAGE)

        return  result == PackageManager.PERMISSION_GRANTED
    }
    private fun requestStoragePermission(){
        val appName = getString(R.string.app_name)
        //permission has been granted
        if(Build.VERSION.SDK_INT >= VERSION_CODES.M && shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE)){
            showDialog("Kids Drawing App","$appName needs access to external storage" )
        }
        //permission has not been granted
        else{
            requestPermission.launch(
                arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            )
        }
    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size : ")
        val smallBtn = brushDialog.findViewById<ImageButton>(R.id.id_small_brush)
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val mediumBtn = brushDialog.findViewById<ImageButton>(R.id.id_medium_brush)
        mediumBtn.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val largeBrush = brushDialog.findViewById<ImageButton>(R.id.id_large_brush)
        largeBrush.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    fun paintClicked(view : View){
        if(view !== mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColorForBrush(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
            )

            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )

            mImageButtonCurrentPaint = view

        }
    }

    private fun showDialog(title : String,message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title).setMessage(message).setPositiveButton("cancel"){
                dialog,_->dialog.dismiss()
        }

        builder.create().show()
        //builder.setCancelable(false) // if the user clicks on some area outside the dialog box on the screen,the dialog box doesn't disappear
    }

    private fun showProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)

        customProgressDialog?.setContentView(R.layout.progress_dialog)

        customProgressDialog?.show()
    }

    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result: String){
        MediaScannerConnection.scanFile(this, arrayOf(result),null){
            _,uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent,"Share"))
        }
    }
}