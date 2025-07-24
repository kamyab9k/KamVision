# `KamVision` :zap:
[![Medium](https://img.shields.io/badge/Medium-12100E.svg)](https://medium.com/@kamyab9k) 
<a href="https://www.linkedin.com/in/kamyab-khosravi-5214551a4/"><img alt="LinkedIn" src="https://img.shields.io/badge/LinkedIn-%230077B5.svg?logo=linkedin&logoColor=white"/></a>
[![API](https://img.shields.io/badge/minSdkVersion-24-important.svg)](https://developer.android.com/tools/releases/platforms#7.0)

**`KamVision`** is a Library that provides easy access to complex Camera 2 API (deliberately chosen over Camera X) in jetpack compose, you can use internal elements like `Preview` and `Frame Capturing` as a one line method call in your projects.
                     <p align="center">  ![KamVisionLogo](https://github.com/user-attachments/assets/e30e4da1-c76c-4be3-b715-aac45efee86b)</p>


<br/>

### Tech Stack

| **Category**               | **Details**                                                                                                                |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| **Core Technologies**      | Kotlin, Java, Coroutines, Flow, MVVM (Model-View-ViewModel), Jetpack Compose                                                     |
| **Design Patterns**        | Singleton, Builder, Facade                                                                                                 |
| **Preview and Image Handling** | Camera 2 Api, AndroidView for preview start/stop, Bitmap handling for single and multiple frames                         |

<br/>

Core Logic
----------
The core of this library is provided by [**Camera2 Api**](https://developer.android.com/media/camera/camera2) & [**Jetpack Compose**](https://developer.android.com/develop/ui/compose/documentation)  .

<br/>


**Why Camera2 API is chosen over Camera X:**
----------
- **Low-Level Control**: Camera2 API provides direct, in-depth control over the camera hardware. You can manipulate almost every aspect of the camera pipeline, including:
- **Manual Exposure Control**: Set ISO, shutter speed, and aperture (if supported by hardware) precisely. This is crucial for creative photography and specific lighting conditions.
- **Manual Focus**: Achieve precise focus control, including setting specific focus distances.
- **RAW Capture**: Access uncompressed RAW sensor data (DNG format), which offers the most flexibility for post-processing.
- **Advanced Capture Request Parameters**: Control a vast array of parameters for each capture, such as white balance, color correction, noise reduction, edge enhancement, tone mapping, and more, using CaptureRequest.Builder.
- **Stream Configuration**: Define custom output streams and formats for different use cases (e.g., high-resolution still image, real-time preview, video recording, image analysis).
- **Metadata Access**: Get detailed metadata for each frame, including sensor timestamps, lens characteristics, and capture settings.
- **Multi-camera Control**: For devices with multiple cameras, Camera2 gives you fine-grained control over which camera to use and how to combine their outputs.
- **Flexibility for Complex Use Cases**: If you're building something beyond standard photo/video capture, like custom computer vision pipelines, augmented reality applications, or specialized imaging tools, Camera2's granular control is essential<br>

**The 👎 of Camera2 api**:
   - It is more complex and time consuming to implement but i've already done it for you ;)


<br>

Main Characteristics
--------------------
- Start Camera Preview with only one line function call
- Stop Camera Preview
- Switch between front and back Camera Lens
- Single frame capturing
- Multiple frame capturing based on a user-defined number
- Full compatibality with jetpack compose
- Fluent UI
- Optimized with coroutines

#### :dart: Download the demostration of the library usage in a sample Android app [KamVision-Showcase.apk](https://github.com/kamyab9k/KamVision/releases/download/v1.0.0/KamVision.v1.0.0.apk)

<br/>

Download
--------
**`KamVision`** is available on `MavenCentral` to download using build tools systems. Add the following lines to your `Version Catalog` and `build.gradle` file:

```gradle

[versions]
kamVision = "1.0.0" 

[libraries]
KamVision = { group = "io.github.kamyab9k", name = "", version.ref = "kamVision" }

dependencies {
    implementation (Libs.)
}
```

<br/>

Usage
-----

`KamVision` makes integrating the Camera2 API into your Jetpack Compose applications simple. Follow these three simple steps to get your camera preview up and running quickly.


1. Request Camera Permissions:
```kotlin
class MainActivity : ComponentActivity() {

    private var cameraPermissionGranted = mutableStateOf(false)
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            cameraPermissionGranted.value = isGranted
            if (!isGranted) {
                // Optionally, inform the user that permission is needed
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val permissionGranted by remember { cameraPermissionGranted }

            LaunchedEffect(Unit) {
                // Check initial permission status
                cameraPermissionGranted.value = isCameraPermissionGranted()
                if (!cameraPermissionGranted.value) {
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }

            if (permissionGranted) {
                CameraPreview()
            } else {
                // Optional: Show a message to the user while waiting for permission
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Camera permission is required to use this app.")
                }
            }
        }
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}
```

2. Integrate KamVision's Camera Preview"
```kotlin
@Composable
fun CameraPreview() {
    val coroutineScope =
        rememberCoroutineScope()
    var cameraService: CameraService? by remember { mutableStateOf(null) }
    var textureView: TextureView? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).also {
                    textureView = it
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { tv ->
                if (cameraService == null) {
                    cameraService = CameraService.Builder(
                        context = tv.context,
                        textureView = tv,
                        scope = coroutineScope
                    )
                        .setFrontCamera(false)
                        .setAutoBrightnessMode(true)
                        .setAutoFocusMode(true)
                        .setFlashMode(2)
                        .build()
                    cameraService?.startPreview()
                }
                cameraService?.captureFrame(50)
                cameraService?.getCapturedFrames { frames: List<Bitmap> ->
                    println("Hi  $frames")
                }
            }
        )
        Button(
            onClick = {
                coroutineScope.launch {
                    cameraService?.switchCamera()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text("Switch Camera")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraService?.stopCameraPreview()
        }
    }
}
```

3. Leverage KamVision Functions:

```kotlin
...
           update = { tv ->
                if (cameraService == null) {
                    cameraService = CameraService.Builder(
                        context = tv.context,
                        textureView = tv,
                        scope = coroutineScope
                   )
            .setFrontCamera(false)       // KamVision parameter: 'true' for front camera, 'false' for back camera
            .setAutoBrightnessMode(true) // KamVision parameter: 'true' to enable auto brightness adjustment, 'false' otherwise
            .setAutoFocusMode(true)      // KamVision parameter: 'true' to enable auto focus, 'false' otherwise
            .setFlashMode(2)             // KamVision parameter: Sets the flash mode (0: OFF, 1: Single Flash, 2: Torch)
            .build()                     // Builds the CameraService instance with the specified configurations
        cameraService?.startPreview()    // KamVision function: Starts the camera preview
    }
    cameraService?.captureFrame(50)      // KamVision function: Captures the specified number of frames (e.g., 50 frames)
    cameraService?.getCapturedFrames { frames: List<Bitmap> -> // KamVision function: Retrieves the list of captured frames
        println("Hi  $frames")
    }
            }
        )
        Button(
            onClick = {
                coroutineScope.launch {
            cameraService?.switchCamera() // KamVision function: Asynchronously switches between front and back cameras
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text("Switch Camera")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraService?.stopCameraPreview()
        }
    }
}
```

<br/>


### Library functions

There are several functions you can call with KamVision.

<br/>

<table>

  <tr>
    <td><b>Function</b></td>
    <td><b>Example Usage</b></td>
  </tr>

  <tr>
    <td><b>• startPreview()</b></td>
    <td>cameraService?.startPreview()
</td>
  </tr>
  <tr>
    <td colspan="2"><i>Starts the camera preview.</i></td>
  </tr>

  <tr>
    <td><b>• stopPreview()</b></td>
    <td>cameraService?.stopPreview()</td>
  </tr>
  <tr>
    <td colspan="2"><i>Stops the camera preview.</i></td>
  </tr>

  <tr>
    <td><b>• captureFrame(Frames: Int)</b></td>
    <td>cameraService?.captureFrame(15)</td>
  </tr>
  <tr>
    <td colspan="2"><i>Capture frames and the number is given in parameter .</i></td>
  </tr>

  <tr>
    <td><b>• getCapturedFrames(frames: List<Bitmap>)</b></td>
    <td>cameraService?.getCapturedFrames { frames: List<Bitmap> ->}</td>
  </tr>
  <tr>
    <td colspan="2"><i>Returns the list of captured frames in Bitmap format.</i></td>
  </tr>




</table>

<br/>

### Input KamVision Configurations

In addition to the builder functions, `KamVision` receives some configurations. For example:

```kotlin
// shows a Preview in jetpack compose AndroidView
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                // Create the TextureView
                TextureView(ctx).also {
                    textureView = it
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { textureView ->
                if (cameraService == null) {
                    cameraService = CameraService.Builder(
                        context = textureView.context,
                        textureView = textureView,
                        scope = coroutineScope
                    ).build()
```

<br/>


Read more about these three important topics to better understand the funcionality of this Library:

- [`Camera2`](https://developer.android.com/media/camera/camera2)
- [`AndroidView`](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/views-in-compose)
- [`Jetpack Compose`](https://developer.android.com/develop/ui/compose/documentation)
<br/>


In Development⚡️
----------

- Video capturing & more
<br/>

License
--------
```
Copyright 2024 Mohammad Khosravi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
