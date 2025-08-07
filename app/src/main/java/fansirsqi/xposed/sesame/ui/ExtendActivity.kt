package fansirsqi.xposed.sesame.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fansirsqi.xposed.sesame.BuildConfig
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.data.DataCache
import fansirsqi.xposed.sesame.entity.ExtendFunctionItem
import fansirsqi.xposed.sesame.ui.widget.ExtendFunctionAdapter
import fansirsqi.xposed.sesame.util.FansirsqiUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.ToastUtil

/**
 * 扩展功能页面
 */
class ExtendActivity : BaseActivity() {
    private val TAG = ExtendActivity::class.java.simpleName
    private var debugTips: String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var extendFunctionAdapter: ExtendFunctionAdapter
    private val extendFunctions = mutableListOf<ExtendFunctionItem>()

    /**
     * 初始化Activity
     *
     * @param savedInstanceState 保存的实例状态
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_extend) // 设置布局文件
        debugTips = getString(R.string.debug_tips)
        baseTitle = getString(R.string.extended_func)

        setupRecyclerView()
        populateExtendFunctions()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView_extend_functions)
        recyclerView.layoutManager = LinearLayoutManager(this)
        extendFunctionAdapter = ExtendFunctionAdapter(extendFunctions)
        recyclerView.adapter = extendFunctionAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun populateExtendFunctions() {
        extendFunctions.add(
            ExtendFunctionItem(getString(R.string.query_the_remaining_amount_of_saplings)) {
                sendItemsBroadcast("getTreeItems")
                ToastUtil.makeText(this@ExtendActivity, debugTips, Toast.LENGTH_SHORT).show()
            }
        )
        extendFunctions.add(
            ExtendFunctionItem(getString(R.string.search_for_new_items_on_saplings)) {
                sendItemsBroadcast("getNewTreeItems")
                ToastUtil.makeText(this@ExtendActivity, debugTips, Toast.LENGTH_SHORT).show()
            }
        )
        extendFunctions.add(
            ExtendFunctionItem(getString(R.string.search_for_unlocked_regions)) {
                sendItemsBroadcast("queryAreaTrees")
                ToastUtil.makeText(this@ExtendActivity, debugTips, Toast.LENGTH_SHORT).show()
            }
        )
        extendFunctions.add(
            ExtendFunctionItem(getString(R.string.search_for_unlocked_items)) {
                sendItemsBroadcast("getUnlockTreeItems")
                ToastUtil.makeText(this@ExtendActivity, debugTips, Toast.LENGTH_SHORT).show()
            }
        )
        extendFunctions.add(
            ExtendFunctionItem(getString(R.string.clear_photo)) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.clear_photo)
                    .setMessage("确认清空${DataCache.getData<List<Map<String, String>>>("guangPanPhoto")?.size ?: 0}组光盘行动图片？")
                    .setPositiveButton(R.string.ok) { _, _ ->
                        if (DataCache.removeData("guangPanPhoto")) {
                            ToastUtil.showToast(this, "光盘行动图片清空成功")
                        } else {
                            ToastUtil.showToast(this, "光盘行动图片清空失败")
                        }
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        )
        //调试功能往里加
        if (BuildConfig.DEBUG) {
            extendFunctions.add(
                ExtendFunctionItem("写入光盘") {
                    AlertDialog.Builder(this)
                        .setTitle("Test")
                        .setMessage("xxxx")
                        .setPositiveButton(R.string.ok) { _, _ ->
                            val newPhotoEntry = HashMap<String, String>()
                            val randomStr = FansirsqiUtil.getRandomString(10)
                            newPhotoEntry["before"] = "before$randomStr"
                            newPhotoEntry["after"] = "after$randomStr"

                            val existingPhotos = DataCache.getData<MutableList<Map<String, String>>>("guangPanPhoto")?.toMutableList() ?: mutableListOf()
                            existingPhotos.add(newPhotoEntry)

                            if (DataCache.saveData("guangPanPhoto", existingPhotos)) {
                                ToastUtil.showToast(this, "写入成功$newPhotoEntry")
                            } else {
                                ToastUtil.showToast(this, "写入失败$newPhotoEntry")
                            }
                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            )

            //我想在这加一个编辑框，里面支持输入文字，下面的展示随机光盘的字段从编辑框里面取

            extendFunctions.add(
                ExtendFunctionItem("获取DataCache字段") {
                    val inputEditText = EditText(this)
                    AlertDialog.Builder(this)
                        .setTitle("输入字段Key")
                        .setView(inputEditText)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            val inputText = inputEditText.text.toString()
                            val output = DataCache.getData<Any>(inputText)
                            ToastUtil.showToast(this, "$output \n输入内容: $inputText")
                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            )


            extendFunctions.add(
                ExtendFunctionItem("获取BaseUrl") {
                    val inputEditText = EditText(this)
                    AlertDialog.Builder(this)
                        .setTitle("请输入Key")
                        .setView(inputEditText)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            val inputText = inputEditText.text.toString()
                            Log.debug(TAG, "获取BaseUrl：$inputText")
                            val key = inputText.toIntOrNull(16)  // 支持输入 0x11 这样的十六进制
                            Log.debug(TAG, "获取BaseUrl key：$key")
                            if (key != null) {
                                val output = getApiUrl(key)
                                ToastUtil.showToast(this, "$output \n输入内容: $inputText")
                            } else {
                                ToastUtil.showToast(this, "输入内容: $inputText , 请输入正确的十六进制数字")
                            }

                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            )
        }
        extendFunctionAdapter.notifyDataSetChanged()
    }

    /**
     * 发送广播事件
     *
     * @param type 广播类型
     */
    private fun sendItemsBroadcast(type: String) {
        val intent = Intent("com.eg.android.AlipayGphone.sesame.rpctest")
        intent.putExtra("method", "")
        intent.putExtra("data", "")
        intent.putExtra("type", type)
        sendBroadcast(intent) // 发送广播
        Log.debug(TAG, "扩展工具主动调用广播查询📢：$type")
    }
}
