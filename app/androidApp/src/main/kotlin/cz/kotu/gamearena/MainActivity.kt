package cz.kotu.gamearena

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mmk.kmpnotifier.permission.permissionUtil

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }

        permissionUtil().value.askNotificationPermission {
            Toast.makeText(this, "Notifications: $it", Toast.LENGTH_SHORT).show()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
