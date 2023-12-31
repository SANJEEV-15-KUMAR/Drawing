package com.example.drawing

import android.app.AlertDialog
import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.get
import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private var drawingView:DrawingView? = null
    private var mImageButtonCurrentPaint:ImageButton? = null

    var customProgressDialog:Dialog? = null


    val requestPermission:ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
                permissions ->
            permissions.entries.forEach{
                val permissionsName = it.key
                val isGranted = it.value

                if(isGranted){
                    Toast.makeText(this@MainActivity,
                        "Permmision granted now you can read the storage files",
                        Toast.LENGTH_SHORT).show()
                    val pickIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)


                }else
                {
                    if(permissionsName == Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(this,
                            "you denied the permmision",
                            Toast.LENGTH_SHORT).show()

                    }
                }
            }


        }



    val openGalleryLauncher:ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if(result.resultCode == RESULT_OK && result.data!=null){
                val imageBackGround:ImageView = findViewById(R.id.iv_background)

                imageBackGround.setImageURI(result.data?.data)

            }
        }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())
        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)

        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
        )


        val ib_brush:ImageButton = findViewById(R.id.ib_brush)
        ib_brush.setOnClickListener {
            showBrushSizeChosserDialog()
        }

        val ib_undo:ImageButton = findViewById(R.id.ib_undo)
        ib_undo.setOnClickListener {
            drawingView?.onClickundo()
        }


        val ibGallery:ImageButton = findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener {

            requestStoragePermission()
        }

        val ibsave:ImageButton = findViewById(R.id.ib_save)
        ibsave.setOnClickListener {

            /*
            if(isReadStorageAllowed()){
                showProgressDialog()
                lifecycleScope.launch {
                    val flDrawingView:FrameLayout = findViewById(R.id.fl_drawing_view_container)
                   // val myBitmap:Bitmap = getBitmapFromView(flDrawingView)
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }

            }

             */

            val flDrawingView : FrameLayout = findViewById(R.id.fl_drawing_view_container)
            saveBitmap(flDrawingView)

        }
    }


    private fun showBrushSizeChosserDialog(){


        var brushDialog = Dialog(this)

        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size:")

       val smallBtn = brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val mediumBtn = brushDialog.findViewById<ImageButton>(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val largeBtn = brushDialog.findViewById<ImageButton>(R.id.ib_btn_large)
        largeBtn.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }



        brushDialog.show()


    }

    fun paintClicked(view: View){

        if(view!==mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
            )

            mImageButtonCurrentPaint?.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )

            mImageButtonCurrentPaint = view


        }

    }

    private fun isReadStorageAllowed():Boolean{

        val  result = ContextCompat.checkSelfPermission(this,
          //  Manifest.permission.READ_EXTERNAL_STORAGE
        Manifest.permission.READ_MEDIA_IMAGES
            )
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission(){

        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,
               Manifest.permission.READ_EXTERNAL_STORAGE
           //     Manifest.permission.READ_MEDIA_IMAGES
        )){
            showRationalDialog("Drawing App","Drawing app needs to Access Your External storage")
        }else
        {
            requestPermission.launch(arrayOf(
            //    Manifest.permission.READ_EXTERNAL_STORAGE,
            //  Manifest.permission.WRITE_EXTERNAL_STORAGE,

             Manifest.permission.READ_MEDIA_IMAGES,
             Manifest.permission.READ_MEDIA_VIDEO

            ))
        }

    }

    private fun showRationalDialog(title:String,message:String){

        val builder:AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("cancle"){ dialog,_ ->
                dialog.dismiss()
            }
        builder.create().show()
    }


    private fun getBitmapFromView(view:View):Bitmap {

        val returnedBitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)

        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable!=null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
         return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBimap:Bitmap?):String{
        var result = ""
        withContext(Dispatchers.IO){
            if(mBimap!=null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBimap.compress(Bitmap.CompressFormat.PNG,90,bytes)

                    val f= File(externalCacheDir?.absoluteFile.toString()
                            +File.separator+"DrawingApp_" +System.currentTimeMillis()/1000 +".png"

                    )

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread{
                        cancleProgressDialolg()
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity, "File saved successfully :$result",
                                Toast.LENGTH_SHORT).show()
                            shareImage(result)
                        }else{
                            Toast.makeText(this@MainActivity,
                                "Something went wrong", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                catch (e:java.lang.Exception){
                    result =""
                    e.printStackTrace()
                }


            }
        }
        return result
    }

    private fun showProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
    }

    private fun cancleProgressDialolg(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result:String){

        MediaScannerConnection.scanFile(this, arrayOf(result),null){
            path,uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent,"Share"))
        }

    }

    private fun saveBitmap(view : View) {
        lifecycleScope.launch {
            val bitmap = getBitmapFromView(view) // replace with your view

            // save bitmap to gallery based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = applicationContext.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "DrawingApp_${System.currentTimeMillis() / 1000}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                    put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                }

                var imageUri: Uri? = null
                resolver.run {
                    imageUri = insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    imageUri?.let {
                        openOutputStream(it)?.use { outputStream ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                        }
                    }
                }

                if (imageUri != null) {
                    Toast.makeText(this@MainActivity, "File saved successfully: $imageUri", Toast.LENGTH_SHORT).show()

                } else {
                    Toast.makeText(this@MainActivity, "Something went wrong while saving the file", Toast.LENGTH_SHORT).show()
                }

            } else {
                val filePath = saveBitmapFile(bitmap)
                if (filePath.isNotEmpty()) {
                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    val file = File(filePath)
                    val contentUri = Uri.fromFile(file)
                    mediaScanIntent.data = contentUri
                    sendBroadcast(mediaScanIntent)
                    Toast.makeText(this@MainActivity, "File saved successfully: $filePath", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Something went wrong while saving the file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


}