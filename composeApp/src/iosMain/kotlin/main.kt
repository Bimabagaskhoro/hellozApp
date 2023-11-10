import androidx.compose.ui.window.ComposeUIViewController
import org.app.projectxyz.hellozapp.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
