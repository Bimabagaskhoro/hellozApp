import androidx.compose.ui.window.ComposeUIViewController
import org.app.projectxyz.hellozapp.feature.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
