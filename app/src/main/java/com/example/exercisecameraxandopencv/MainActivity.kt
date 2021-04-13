package com.example.exercisecameraxandopencv

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Executors.newSingleThreadExecutor

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

    private fun startCamera() {}

    // ユーザーがすでに特定の権限をアプリに付与しているかをチェックする．
    // そのためにその権限をContextCompat.checkSelfPermission()メソッドに渡す．
    // 権限がある場合PERMISSION_GRANTEDを返す．
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                baseContext, it) == PackageManager.PERMISSION_GRANTED

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