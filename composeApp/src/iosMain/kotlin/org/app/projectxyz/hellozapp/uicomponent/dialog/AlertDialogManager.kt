package org.app.projectxyz.hellozapp.uicomponent.dialog

import androidx.compose.ui.window.DialogProperties
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertActionStyleDestructive
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UITapGestureRecognizer
import platform.objc.sel_registerName

class AlertDialogManager internal constructor(
    internal var onConfirm: () -> Unit,
    internal var onDismiss: () -> Unit,
    internal var confirmText: String,
    internal var dismissText: String,
    internal var title: String,
    internal var text: String,
    internal var properties: DialogProperties,
) {
    private var isPresented = false
    private var isAnimating = false
    private val onDismissLambda: (() -> Unit) = {
        UIApplication.sharedApplication.keyWindow?.rootViewController?.dismissViewControllerAnimated(
            flag = true,
            completion = {
                isPresented = false
                isAnimating = false
                onDismiss()
            }
        )
    }
    @OptIn(ExperimentalForeignApi::class)
    private val dismissPointer = sel_registerName("dismiss")
    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun dismiss() {
        if (!isPresented || isAnimating) return
        isAnimating = true
        onDismissLambda.invoke()
    }
    @OptIn(ExperimentalForeignApi::class)
    fun showAlertDialog() {
        if (isPresented || isAnimating) return
        isAnimating = true

        val alertController = UIAlertController.alertControllerWithTitle(
            title = title,
            message = text,
            preferredStyle = UIAlertControllerStyleAlert
        )

        val confirmAction = UIAlertAction.actionWithTitle(
            title = confirmText,
            style = UIAlertActionStyleDefault,
            handler = {
                onConfirm()
            }
        )
        alertController.addAction(confirmAction)

        val cancelAction = UIAlertAction.actionWithTitle(
            title = dismissText,
            style = UIAlertActionStyleDestructive,
            handler = {
                onDismiss()
            }
        )
        alertController.addAction(cancelAction)

        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
            viewControllerToPresent = alertController,
            animated = true,
            completion = {
                isPresented = true
                isAnimating = false

                if (properties.dismissOnClickOutside) {
                    alertController.view.superview?.setUserInteractionEnabled(true)
                    alertController.view.superview?.addGestureRecognizer(
                        UITapGestureRecognizer(
                            target = this,
                            action = dismissPointer
                        )
                    )
                }
            }
        )
    }
}