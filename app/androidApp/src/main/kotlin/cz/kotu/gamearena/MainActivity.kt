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
    private val kmpNotifierPermissionUtil by permissionUtil()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Component retrieved from Application context
        val appComponent: AppComponent = (applicationContext as GameArenaApplication).appComponent

        // init early to avoid
        // java.lang.IllegalStateException: LifecycleOwner cz.kotu.gamearena.MainActivity is attempting to register while current state is RESUMED. LifecycleOwners must call register before they are STARTED.
        kmpNotifierPermissionUtil

        setContent {
            App(
                appComponent = appComponent,
                askNotificationPermission = ::askNotificationPermission,
            )
        }
    }

    private fun askNotificationPermission(onPermissionResult: (isGranted: Boolean) -> Unit) {
        kmpNotifierPermissionUtil.askNotificationPermission {
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
