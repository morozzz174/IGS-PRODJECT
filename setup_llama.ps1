# Setup script for llama.cpp native library
# Run this from the project root (C:\IGS)

$ErrorActionPreference = "Stop"
$ProjectRoot = Get-Location
$CppDir = Join-Path $ProjectRoot "app\src\main\cpp"
$LlamaDir = Join-Path $CppDir "llama.cpp"

Write-Host "=== Setting up llama.cpp for Android ===" -ForegroundColor Cyan

# Step 1: Clone llama.cpp
if (Test-Path $LlamaDir) {
    Write-Host "llama.cpp directory already exists. Updating..." -ForegroundColor Yellow
    Set-Location $LlamaDir
    git pull
    Set-Location $ProjectRoot
} else {
    Write-Host "Cloning llama.cpp..." -ForegroundColor Green
    git clone --depth 1 https://github.com/ggerganov/llama.cpp.git $LlamaDir
}

$BuildDir = Join-Path $LlamaDir "build"

# Step 2: Create CMake toolchain file for Android
$ToolchainPath = "$env:ANDROID_HOME\ndk\26.1.10909125\build\cmake\android.toolchain.cmake"
if (-not (Test-Path $ToolchainPath)) {
    # Try common NDK locations
    $possiblePaths = @(
        "$env:ANDROID_HOME\ndk\*\build\cmake\android.toolchain.cmake",
        "$env:ANDROID_SDK_ROOT\ndk\*\build\cmake\android.toolchain.cmake",
        "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\ndk\*\build\cmake\android.toolchain.cmake"
    )
    foreach ($pattern in $possiblePaths) {
        $matches = Resolve-Path $pattern -ErrorAction SilentlyContinue
        if ($matches) {
            $ToolchainPath = $matches[-1].Path
            break
        }
    }
}

if (-not (Test-Path $ToolchainPath)) {
    Write-Host "Warning: Android NDK toolchain not found. Set ANDROID_HOME environment variable." -ForegroundColor Yellow
    Write-Host "Expected at: $env:ANDROID_HOME\ndk\26.1.10909125\build\cmake\android.toolchain.cmake" -ForegroundColor Yellow
    Write-Host "You can manually build llama.cpp later." -ForegroundColor Yellow
} else {
    Write-Host "Android NDK found: $ToolchainPath" -ForegroundColor Green

    # Step 3: Build llama.cpp for arm64-v8a (most common Android architecture)
    Write-Host "Building llama.cpp for arm64-v8a..." -ForegroundColor Green

    if (-not (Test-Path $BuildDir)) {
        New-Item -ItemType Directory -Path $BuildDir -Force
    }

    Set-Location $BuildDir
    
    & cmake `
        -G "Ninja" `
        -DCMAKE_TOOLCHAIN_FILE="$ToolchainPath" `
        -DANDROID_ABI="arm64-v8a" `
        -DANDROID_PLATFORM=android-26 `
        -DCMAKE_BUILD_TYPE=Release `
        -DLLAMA_BUILD_TESTS=OFF `
        -DLLAMA_BUILD_EXAMPLES=OFF `
        -DLLAMA_BUILD_SERVER=OFF `
        -DBUILD_SHARED_LIBS=ON `
        ..

    if ($LASTEXITCODE -eq 0) {
        & cmake --build . --config Release
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Build successful!" -ForegroundColor Green
            
            # Copy built libraries to the project
            $LibsDir = Join-Path $ProjectRoot "app\src\main\jniLibs\arm64-v8a"
            if (-not (Test-Path $LibsDir)) {
                New-Item -ItemType Directory -Path $LibsDir -Force
            }
            
            Copy-Item (Join-Path $BuildDir "libllama.so") -Destination $LibsDir -Force
            Write-Host "Copied libllama.so to $LibsDir" -ForegroundColor Green
        } else {
            Write-Host "Build failed!" -ForegroundColor Red
        }
    } else {
        Write-Host "CMake configuration failed!" -ForegroundColor Red
    }
    
    Set-Location $ProjectRoot
}

Write-Host ""
Write-Host "=== Setup complete ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Open this project in Android Studio" -ForegroundColor Yellow
Write-Host "2. Build > Make Project (Ctrl+F9)" -ForegroundColor Yellow
Write-Host "3. The llama_jni library will be built automatically" -ForegroundColor Yellow
Write-Host ""
Write-Host "Or build manually:" -ForegroundColor Yellow
Write-Host "  cd app/src/main/cpp/llama.cpp" -ForegroundColor Yellow
Write-Host "  mkdir build && cd build" -ForegroundColor Yellow
Write-Host '  cmake -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-26 ..' -ForegroundColor Yellow
Write-Host "  cmake --build ." -ForegroundColor Yellow
