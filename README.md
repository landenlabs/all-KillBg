# LanDen Labs - all-KillBg
<br>18-Arp-2026
<br>API 36 AndroidX Java
<br>[Home website](https://landenlabs.com/android/index.html)

<img src="screenshots/landenlabs.webp" width="300" alt="Logo">

An Android utility application to manage and kill background processes, view process details, and monitor system memory.

## Features

- **Process Management**: View a list of running processes and packages.
- **Kill Background Tasks**: Easily terminate background processes to free up memory.
- **Kill List**: Maintain a list of specific processes to be targeted.
- **Process Details**: View in-depth information about individual processes.
- **Memory Monitor**: Real-time display of available system memory.
- **Modern Android Stack**: Updated to use the latest Android tools and practices.

## Technical Specifications

- **Language**: Pure Java (Kotlin-free)
- **Minimum SDK**: 21 (Android 5.0)
- **Target/Compile SDK**: 36 (Android 16)
- **Java Version**: 17 (Toolchain)
- **Build System**: Gradle 9.4.1 with Version Catalog support
- **AGP**: 9.1.1

## Key Dependencies

- `androidx.appcompat`: For modern UI components.
- `com.jaredrummler:android-processes`: For advanced process management.

## Project Structure

- `app/`: Main application module.
- `gradle/libs.versions.toml`: Centralized dependency management.

## Development

The project has been modernized to use:
- **Gradle 9.x**: Utilizing the latest performance improvements and configuration cache.
- **New Variant API**: Uses `androidComponents` for APK renaming and resource value injection.
- **Java 17**: Leverages modern Java features and performance.

## License

Copyright (c) 2026 Dennis Lang (LanDen Labs)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

---
Author: Dennis Lang  
Website: [landenLabs.com](https://landenLabs.com/)
