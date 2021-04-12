package com.example.exercisecameraxandopencv

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.util.concurrent.ExecutorService

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
    }
}