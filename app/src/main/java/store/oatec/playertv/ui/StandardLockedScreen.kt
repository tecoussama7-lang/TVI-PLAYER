package store.oatec.playertv.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import store.oatec.playertv.data.StandardEntitlementClient

@Composable
fun StandardLockedScreen(context: Context, expiresAt: String, paymentUrl: String, onRetry: () -> Unit) {
    var reference by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    var busy by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Surface(Modifier.fillMaxSize(), color = Color(0xFF05070A)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("OATEC STANDARD", color = Color(0xFFFFDA72), fontSize = 23.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Text("انتهت فترة التجربة المجانية", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("التفعيل الدائم للجهاز بثمن 70 درهم. افتح Streamshop وأكمل الأداء، ثم أدخل رقم العملية لإرسال طلب المراجعة.", color = Color(0xFF9DA5B1), fontSize = 14.sp)
            if (expiresAt.isNotBlank()) Text("نهاية التجربة: $expiresAt", color = Color(0xFF9DA5B1), fontSize = 12.sp)
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    val url = paymentUrl.ifBlank { "https://oatec.store/" }
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8B83E), contentColor = Color(0xFF05070A)),
            ) { Text("فتح Streamshop والأداء — 70 درهم", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(reference, { reference = it; message = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("رقم مرجع الأداء") }, singleLine = true)
            OutlinedTextField(contact, { contact = it; message = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("الهاتف أو البريد (اختياري)") }, singleLine = true)
            if (message.isNotBlank()) Text(message, color = Color(0xFFFFDA72), fontSize = 13.sp)
            Button(
                onClick = {
                    if (reference.isBlank()) message = "دخل رقم مرجع الأداء أولاً."
                    else {
                        busy = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { StandardEntitlementClient(context).submitPayment(reference, contact) }
                                message = "توصلنا بطلبك. غادي تتم مراجعة الأداء قبل تفعيل الجهاز."
                            } catch (e: Exception) {
                                message = e.message ?: "تعذر إرسال الطلب."
                            } finally { busy = false }
                        }
                    }
                },
                enabled = !busy && reference.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF292F39), contentColor = Color.White),
            ) { if (busy) CircularProgressIndicator() else Text("إرسال طلب مراجعة الأداء") }
            TextButton(onClick = onRetry) { Text("تحقق من حالة التفعيل من جديد", color = Color(0xFFFFDA72)) }
        }
    }
}
