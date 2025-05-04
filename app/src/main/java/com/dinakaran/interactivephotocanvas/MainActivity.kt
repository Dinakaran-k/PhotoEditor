package com.dinakaran.interactivephotocanvas

import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dinakaran.interactivephotocanvas.ui.screens.CameraScreen
import com.dinakaran.interactivephotocanvas.ui.screens.EditPhotoScreen
import com.dinakaran.interactivephotocanvas.ui.theme.InteractivePhotoCanvasTheme
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {

    private val cameraPermissionState = mutableStateOf(false)
    private val storagePermissionState = mutableStateOf(true)

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        cameraPermissionState.value = permissions[CAMERA] == true
        storagePermissionState.value = permissions[WRITE_EXTERNAL_STORAGE] != false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Request Permission
        val permissionsToRequest = mutableListOf<String>()
        if(ContextCompat.checkSelfPermission(this,CAMERA) != PackageManager.PERMISSION_GRANTED){
            permissionsToRequest.add(CAMERA)
        }
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P
            &&  ContextCompat.checkSelfPermission(this,WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permissionsToRequest.add(WRITE_EXTERNAL_STORAGE)
        } else {
            storagePermissionState.value = true
        }

        if (permissionsToRequest.isNotEmpty()){
            cameraPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            cameraPermissionState.value = true
        }

        enableEdgeToEdge()
        setContent {
            InteractivePhotoCanvasTheme {


                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "camera"
                ) {
                    composable("camera") {
                        if (cameraPermissionState.value) {
                            CameraScreen(
                                onPhotoCaptured = { uri ->
                                    val encodedUri = URLEncoder.encode(uri, "UTF-8")
                                    navController.navigate("editPhoto/$encodedUri")
                                }
                            )
                        } else {
                            Text("Camera permission is required")
                        }
                    }
                    composable(
                        route = "editPhoto/{uri}",
                        arguments = listOf(navArgument("uri") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val encodedUri = backStackEntry.arguments?.getString("uri") ?: ""
                        val uri = URLDecoder.decode(encodedUri, "UTF-8")
                        EditPhotoScreen(
                            uri = uri,
                            onBack = { navController.popBackStack() },
                            storagePermissionGranted = storagePermissionState.value
                        )
                    }
                }
            }
        }
    }
}