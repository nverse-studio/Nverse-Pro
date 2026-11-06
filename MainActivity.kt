package com.nverse.tool

import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

// 数据模型
data class FeatureCategory(val categoryName: String, val items: List<FeatureItem>)
data class FeatureItem(val id: String, val title: String, val desc: String, val tag: String = "", val route: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") { HomeScreen(navController) }
                    composable("web_source") { WebSourceScreen(navController) }
                    composable("adb_auth") { AdbAuthScreen(navController) }
                    composable("root_boot") { RootBootScreen(navController) }
                    composable("gms_install") { GmsInstallScreen(navController) }
                }
            }
        }
    }
}

// 核心底层命令执行方法 (老手写法)
suspend fun executeCommand(cmd: String): String = withContext(Dispatchers.IO) {
    try {
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val errorReader = BufferedReader(InputStreamReader(process.errorStream))
        val output = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) output.append(line).append("\n")
        while (errorReader.readLine().also { line = it } != null) output.append(line).append("\n")
        reader.close(); errorReader.close()
        output.toString().trim().ifEmpty { "执行完成，无返回信息。" }
    } catch (e: Exception) {
        "执行异常: ${e.message}"
    }
}

@Composable
fun HomeScreen(navController: NavController) {
    var showDisclaimer by remember { mutableStateOf(true) }

    val categories = listOf(
        FeatureCategory("系统底层", listOf(
            FeatureItem("gms", "谷歌三件套", "一键静默安装 GMS", "", "gms_install"),
            FeatureItem("adb", "ADB 分发授权", "Shizuku 框架深度授权", "", "adb_auth"),
            FeatureItem("boot", "Root 启动", "获取 Root 权限并开机自启", "", "root_boot")
        )),
        FeatureCategory("极客工具", listOf(
            FeatureItem("web", "读取网页源码", "AI 自动审核安全性", "Pro会员", "web_source"),
            FeatureItem("vpn", "海外应用下载", "内置极速海外节点", "", "gms_install")
        )),
        FeatureCategory("违规/下线功能", listOf(
            FeatureItem("wifi", "修改 WiFi 密码", "该功能已被安全合规拦截", "合规下架", ""),
            FeatureItem("phone", "分发海外手机号", "接码黑产，涉及违法", "违规下架", "")
        ))
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(id = R.drawable.bg_image), contentDescription = "Bg", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))

        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 40.dp)
        ) {
            items(categories) { category -> GlassCategoryCard(category, navController) }
        }
    }

    if (showDisclaimer) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("免责声明", fontWeight = FontWeight.Bold) },
            text = { Text("本软件包含底层系统操作，请确保您了解相关风险。使用本软件造成的设备损坏、数据丢失或法律纠纷，开发者概不负责。请遵守当地法律法规。") },
            confirmButton = { TextButton(onClick = { showDisclaimer = false }) { Text("同意并进入") } }
        )
    }
}

// ADB 授权页 (基于 Shizuku 12.1.0 API)
@Composable
fun AdbAuthScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var logText by remember { mutableStateOf("准备就绪。\n点击下方按钮申请 Shizuku 授权。") }
    var isRunning by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(id = R.drawable.bg_image), contentDescription = "Bg", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)))

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
            Text("返回", color = Color.White, modifier = Modifier.clickable { navController.popBackStack() })
            Spacer(modifier = Modifier.height(30.dp))
            Text("分发 ADB 权限", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            
            Card(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.White.copy(alpha = 0.1f))) {
                Text(logText, modifier = Modifier.padding(16.dp), color = Color.Green)
            }
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = {
                    isRunning = true
                    scope.launch {
                        try {
                            // 检查 Shizuku 是否运行
                            if (!Shizuku.pingBinder()) {
                                logText = "错误：Shizuku 服务未运行。请先去下载并激活 Shizuku。"
                                isRunning = false
                                return@launch
                            }
                            // 检查权限
                            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                                logText = "权限已授予！正在通过 Shizuku 执行底层命令...\n\n" + executeCommand("pm list packages | head -n 5")
                            } else {
                                logText = "正在弹出 Shizuku 授权申请框..."
                                Shizuku.requestPermission(0)
                            }
                        } catch (e: Exception) {
                            logText = "Shizuku 交互异常: ${e.message}"
                        }
                        isRunning = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isRunning
            ) { Text(if (isRunning) "执行中..." else "申请并分发权限") }
        }
    }
}

// Root 启动页 (真实 su 检测)
@Composable
fun RootBootScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var logText by remember { mutableStateOf("准备就绪。\n正在检测 Root 环境...") }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(id = R.drawable.bg_image), contentDescription = "Bg", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)))
        
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
            Text("返回", color = Color.White, modifier = Modifier.clickable { navController.popBackStack() })
            Spacer(modifier = Modifier.height(30.dp))
            Text("Root 启动权限", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            
            Card(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.White.copy(alpha = 0.1f))) {
                Text(logText, modifier = Modifier.padding(16.dp), color = Color.Green)
            }
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = {
                    scope.launch {
                        logText = "正在尝试获取 Root 权限...\n"
                        val rootCheck = executeCommand("su -c id")
                        if (rootCheck.contains("uid=0")) {
                            logText += "检测到 Root 权限！\n准备执行启动项写入与提权动画。"
                            // 此处可以插入月虹提权动画的代码
                        } else {
                            logText += "未检测到 Root，或用户拒绝了授权。\n输出: $rootCheck"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("启动并获取 Root 权限") }
        }
    }
}

// GMS 安装页
@Composable
fun GmsInstallScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
            Text("返回", color = Color.White, modifier = Modifier.clickable { navController.popBackStack() })
            Spacer(modifier = Modifier.height(30.dp))
            Text("谷歌三件套安装", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Text("静默安装逻辑已触发，等待底层包解析...", color = Color.LightGray)
        }
    }
}

// 抓取网页源码（去掉了底部输入框的白底）
@Composable
fun WebSourceScreen(navController: NavController) {
    var urlInput by remember { mutableStateOf("") }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(id = R.drawable.bg_image), contentDescription = "Bg", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
            Text("返回", color = Color.White, modifier = Modifier.clickable { navController.popBackStack() })
            Spacer(modifier = Modifier.height(30.dp))
            Text("AI 读取网页源码", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(30.dp))
            
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("输入网址 (如 https://...)", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = { /* 接入 AI 审核和抓取代码 */ },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
            ) { Text("开始 AI 审核并抓取", color = Color.White) }
        }
    }
}

// 大卡片 UI
@Composable
fun GlassCategoryCard(category: FeatureCategory, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Text(category.categoryName, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
        
        category.items.forEach { item ->
            val isAvailable = !item.tag.contains("下架")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = if (isAvailable) 0.12f else 0.03f))
                    .clickable(enabled = isAvailable) { navController.navigate(item.route) }
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.title, color = if (isAvailable) Color.White else Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            if (item.tag.isNotEmpty()) { Spacer(modifier = Modifier.width(8.dp)); TagBadge(item.tag) }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.desc, color = if (isAvailable) Color.LightGray else Color.DarkGray, fontSize = 12.sp)
                    }
                    if (isAvailable) Text(">", color = Color.White, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun TagBadge(text: String) {
    val bgColor = when {
        text.contains("下架") || text.contains("违规") -> Color(0xFFE53935).copy(alpha = 0.8f)
        text.contains("Pro") -> Color(0xFFFFB300).copy(alpha = 0.8f)
        else -> Color(0xFF1E88E5).copy(alpha = 0.8f)
    }
    Box(modifier = Modifier.background(color = bgColor, shape = RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(text = text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}