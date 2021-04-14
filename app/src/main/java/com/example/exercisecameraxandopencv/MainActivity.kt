package com.example.exercisecameraxandopencv

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    /*
     ExecutorService
     スレッドプールを用いてタスクを実行する
     [タスク(Runnable)]
     1. created: サブミットされていない状態のタスク
     2. submitted: submitまたはexecuteメソッドでタスクをサブミットする
     3. started: 処理が開始した
     4. completed: 処理が完了した
     */
    private lateinit var cameraExecutor: ExecutorService


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ライブラリの読み込み
        OpenCVLoader.initDebug()

        // カメラのパーミッションがあるならカメラを起動する．
        // ないならリクエストを送る
        if(allPermissionsGranted()) {
            startCamera()
        } else {
            /*
             ActivityCompat.requestPermissionsでパーミッションダイアログを表示する．
             パーミッションダイアログの結果はonRequestPermissionsResultをオーバーライドする．
             第1引数：Activityのインスタンス
             第2引数：利用したいパーミッションの文字列配列
             第3引数：リクエストコード
             */
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Executorのインスタンス生成(単一スレッド)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }


    // カメラの起動
    private fun startCamera() {
        // ProcessCameraProviderのインスタンス生成
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // リスナーの設定
        // 第1引数：Runnable（タスク）
        // 第2引数：ContextCompat.getMainExecutor()
        //         メインスレッドで動いているExecutorを返す．
        cameraProviderFuture.addListener(Runnable {
            // カメラのライフサイクルをlifecycleOwnerにバインドするために使う．
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // previewの作成
            val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(preview_view_main.createSurfaceProvider())
                    }

            val imageAnalyzer = ImageAnalysis.Builder()
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, MyImageAnalyzer())
                    }

            // cameraSelector: 背面カメラをデフォルトで利用する．
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // cameraProviderのバインドを切る
                cameraProvider.unbindAll()
                // ライフサイクルとバインドする．
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalyzer)
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // ユーザーがすでに特定の権限をアプリに付与しているかをチェックする．
    // そのためにその権限をContextCompat.checkSelfPermission()メソッドに渡す．
    // 権限がある場合PERMISSION_GRANTEDを返す．
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                baseContext, it) == PackageManager.PERMISSION_GRANTED

    }

    // リクエストを受け取る
    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) {
        // リクエストコードが正しいかチェックして，正しかったらカメラを起動する．
        // そうでないならテキストを表示する．
        if(requestCode == REQUEST_CODE_PERMISSIONS) {
            if(allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // inner classにしないと外部変数にアクセスできない
    inner class MyImageAnalyzer() : ImageAnalysis.Analyzer {
        // 前のmatデータ
        private var matPrevious: Mat? = null

        override fun analyze(image: ImageProxy) {
            // create cv::mat(RGB888) from image(NV21)
            val matOrg = getMatFromImage(image)

            // 回転処理
            val mat = fixMatRotation(matOrg)

            Log.i(TAG, "[analyze] width = ${image.width}, height = ${image.height}, Rotation = ${preview_view_main.display.rotation}")
            Log.i(TAG, "[analyze] mat width = ${matOrg.cols()}, mat height = ${matOrg.rows()}")

            // 画像処理：差分検出
            // 出力データ
            val matOutput = Mat(mat.rows(), mat.cols(), mat.type())
            // 前のデータ（差分対象）のチェック
            if(matPrevious == null) matPrevious = mat
            Core.absdiff(mat, matPrevious, matOutput)
            matPrevious = mat

            // convert cv::mat to bitmap for drawing
            val bitmap = Bitmap.createBitmap(matOutput.cols(), matOutput.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(matOutput, bitmap)

            runOnUiThread { image_view_main.setImageBitmap(bitmap) }

            // 終了（終了しないと次回呼ばれない）
            image.close()
        }

        private fun getMatFromImage(image: ImageProxy): Mat {
            val yBuffer: ByteBuffer = image.planes[0].buffer
            val uBuffer: ByteBuffer = image.planes[1].buffer
            val vBuffer: ByteBuffer = image.planes[2].buffer
            val ySize: Int = yBuffer.remaining()
            val uSize: Int = uBuffer.remaining()
            val vSize: Int = vBuffer.remaining()
            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            val yuv = Mat(image.height + image.height / 2, image.width, CvType.CV_8UC1)
            yuv.put(0, 0, nv21)
            val mat = Mat()
            Imgproc.cvtColor(yuv, mat, Imgproc.COLOR_YUV2RGB_NV21, 3)
            return mat
        }

        private fun fixMatRotation(matOrg: Mat): Mat {
            val mat: Mat
            when (preview_view_main.display.rotation) {
                Surface.ROTATION_0 -> {
                    mat = Mat(matOrg.cols(), matOrg.rows(), matOrg.type())
                    Core.transpose(matOrg, mat)
                    Core.flip(mat, mat, 1)
                }
                Surface.ROTATION_90 -> mat = matOrg
                Surface.ROTATION_270 -> {
                    mat = matOrg
                    Core.flip(mat, mat, -1)
                }
                else -> {
                    mat = Mat(matOrg.cols(), matOrg.rows(), matOrg.type())
                    Core.transpose(matOrg, mat)
                    Core.flip(mat, mat, 1)
                }
            }
            return mat
        }

    }

    // activity終了時にcameraExecutorも終了する
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    }
}