# `KamVision` :zap:
[![Medium](https://img.shields.io/badge/Medium-12100E.svg)](https://medium.com/@kamyab9k) [![Read my article](https://img.shields.io/badge/Read%20my%20article-brightgreen.svg)](https://medium.com/@kamyab9k)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c1c44ee8a3a14b0e8c963c36c8e586d8)](https://app.codacy.com/manual/aminography/PrimeDatePicker?utm_source=github.com&utm_medium=referral&utm_content=aminography/PrimeDatePicker&utm_campaign=Badge_Grade_Dashboard)
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

To enjoy `KamVision` in jetpack compose, create an instance using a builder pattern in simple 4 steps.

1. First create an  **AndroidView** and **TextureView** in a Box(Composable) to later pass it to the Library function parameters:
```kotlin
// To create textureView to pass later on
var textureView: TextureView? by remember { mutableStateOf(null) }

@Composable
fun CameraPreview() {
    val coroutineScope =
        rememberCoroutineScope()
    var textureView: TextureView? by remember { mutableStateOf(null) }

    // Use Box to ensure that the preview fills the entire screen
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
                   }
            }
        )
    }
}
```

2. Create instance of the Library:
```kotlin
    var cameraService: CameraService? by remember { mutableStateOf(null) }

    var textureView: TextureView? by remember { mutableStateOf(null) }
...
```

3. Build the Lib in the update block :

```kotlin
...

 update = { textureView ->
                if (cameraService == null) {
                    cameraService = CameraService.Builder(
                        context = textureView.context,
                        textureView = textureView,
                        scope = coroutineScope
                    ).build()
...
```

4. finally call the Library functions:
```kotlin
...
                    cameraService?.startPreview()

                    cameraService?.captureFrame(20)

                    cameraService!!.getCapturedFrames { frames: List<Bitmap> ->
//                  Handle captured frames here

}
...
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
