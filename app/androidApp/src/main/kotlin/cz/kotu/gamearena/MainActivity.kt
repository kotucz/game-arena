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

        // Component retrieved from Application context
        val appComponent: AppComponent = (applicationContext as GameArenaApplication).appComponent

        setContent {
            App(
                appComponent = appComponent,
                askNotificationPermission = ::askNotificationPermission,
            )
        }
    }

    private fun askNotificationPermission(onPermissionResult: (isGranted: Boolean) -> Unit) {
        permissionUtil().value.askNotificationPermission {
            Toast.makeText(this, "Notifications: $it", Toast.LENGTH_SHORT).show()
            onPermissionResult(it)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
