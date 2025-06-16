# `KamVision` :zap:
[![Medium](https://img.shields.io/badge/Medium-12100E.svg)](https://medium.com/@kamyab9k) [![Read my article](https://img.shields.io/badge/Read%20my%20article-brightgreen.svg)](https://medium.com/@kamyab9k)
![Codacy Badge](https://api.codacy.com/project/badge/Grade/c1c44ee8a3a14b0e8c963c36c8e586d8)
[![API](https://img.shields.io/badge/minSdkVersion-24-important.svg)](https://developer.android.com/tools/releases/platforms#7.0)

**`KamVision`** is a Library that provides easy access to complex Camera 2 API in jetpack compose, you can use internal elements like `Preview` and `Frame Capturing` as a one line method call in your projects.
                       ![KamVisionLogo](https://github.com/user-attachments/assets/e30e4da1-c76c-4be3-b715-aac45efee86b)


<br/>

### Tech Stack

| **Category**               | **Details**                                                                                                                |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| **Core Technologies**      | Kotlin, Java, Coroutines, Flow, MVVM (Model-View-ViewModel), Jetpack Compose                                                     |
| **Design Patterns**        | Singleton, Builder, Facade                                                                                                 |
| **Preview and Image Handling** | Camera 2 Api, AndroidView for preview start/stop, Bitmap handling for single and multiple frames                         |

<br/>


Table of Contents
-----------------

- [Core Logic](#core-logic)
- [Main Characteristics](#main-characteristics)
- [Download](#download)
- [Usage](#usage)
   - [Library functions](#Library-functions)
   - [Input KamVision Configurations](#input-KamVision-configurations)
<br/>






Core Logic
----------
The ❤️ of this library is provided by [**Camera2 Api**](https://developer.android.com/media/camera/camera2) & [**Jetpack Compose**](https://developer.android.com/develop/ui/compose/documentation)  .

<br/>

Main Characteristics
--------------------
- Start Camera Preview with only one line function call
- Stop Camera Preview
- Single frame capturing
- Multiple frame capturing
- Full compatibality with jetpack compose
- Fluent UI
- Optimized with coroutines

#### :dart: Download [SampleApp.apk](https://github.com/kamyab9k/KamVision/releases/download/sample-app-release.apk)

<br/>

Download
--------
**`KamVision`** is available on `MavenCentral` to download using build tools systems. Add the following lines to your `build.gradle` file:

```gradle
dependencies {
    implementation (Libs.)
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

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Handle permission result if needed, but the LaunchedEffect will re-evaluate
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val cameraPermissionGranted = remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                cameraPermissionGranted.value = isCameraPermissionGranted()
                if (!cameraPermissionGranted.value) {
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }

            if (cameraPermissionGranted.value) {
                CameraPreview()
            } else {
                Text("Camera permission is required to use this feature.")
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
    val coroutineScope = rememberCoroutineScope()
    var cameraService: CameraService? by remember { mutableStateOf(null) }
    var textureView: TextureView? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                // Create the TextureView that KamVision will use
                TextureView(ctx).also {
                    textureView = it
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { currentTextureView ->
                // Initialize CameraService using its Builder pattern
                if (cameraService == null) {
                    cameraService = CameraService.Builder(
                        context = currentTextureView.context,
                        textureView = currentTextureView,
                        scope = coroutineScope
                    )
                        .setFrontCamera(true) // Set to 'true' for front camera, 'false' for back
                        .build()
                }
            }
        )
    }
}
```

3. Leverage KamVision Functions:

```kotlin
...
            modifier = Modifier.fillMaxSize(),
            update = { currentTextureView ->
                if (cameraService == null) {
                    cameraService = CameraService.Builder(
                        context = currentTextureView.context,
                        textureView = currentTextureView,
                        scope = coroutineScope
                    )
                        .setFrontCamera(true)
                        .build()

                    // Start the camera preview once the service is built
                    cameraService?.startPreview()
                }

                // Example: Capture 50 frames
                cameraService?.captureFrame(50)

                // Example: Get captured frames
                cameraService?.getCapturedFrames { frames: List<Bitmap> ->
                    println("Hi  $frames")
                    println("Hi, captured ${frames.size} frames!")
                    // Process your captured frames here
                }
            }
        )

        // Example: Add a button to switch cameras
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

    // Stop the camera preview when the composable leaves the composition
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


In Development
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
