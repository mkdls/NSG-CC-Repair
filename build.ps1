param(
 [string]$Sdk = $(if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA/Android/Sdk" }),
 [string]$JavaHome = $env:JAVA_HOME,
 [string]$BuildTools = '36.0.0',
 [string]$BuildDir = "$PSScriptRoot/.build",
 [string]$ApiJar = '',
 [string]$Keystore = '',
 [string]$KeyAlias = 'androiddebugkey',
 [string]$StorePasswordEnv = 'CC_REPAIR_STORE_PASSWORD',
 [string]$KeyPasswordEnv = 'CC_REPAIR_KEY_PASSWORD'
)
$ErrorActionPreference = 'Stop'
if (!$JavaHome) { throw 'Set JAVA_HOME to a JDK 21 installation, or pass -JavaHome.' }
$bt = Join-Path $Sdk "build-tools/$BuildTools"
$android = Join-Path $Sdk 'platforms/android-36/android.jar'
foreach ($required in @($android, "$JavaHome/bin/javac.exe", "$bt/aapt2.exe")) {
 if (!(Test-Path -LiteralPath $required)) { throw "Required tool not found: $required" }
}
$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaHome/bin;$env:PATH"
New-Item -ItemType Directory -Force "$BuildDir/classes", "$BuildDir/dex", "$BuildDir/deps", "$PSScriptRoot/dist" | Out-Null
$BuildDir = (Resolve-Path $BuildDir).Path
function Checked { param([string]$Exe,[string[]]$Arguments) & $Exe @Arguments; if($LASTEXITCODE -ne 0) { throw "$Exe failed: $LASTEXITCODE" } }
Add-Type -AssemblyName System.IO.Compression.FileSystem
if (!$ApiJar) {
 $aar = "$BuildDir/deps/api-102.0.0.aar"
 if (!(Test-Path -LiteralPath $aar)) { Invoke-WebRequest 'https://repo.maven.apache.org/maven2/io/github/libxposed/api/102.0.0/api-102.0.0.aar' -OutFile $aar }
 if ((Get-FileHash -LiteralPath $aar -Algorithm SHA256).Hash.ToLowerInvariant() -ne '423484a6e1807e7a423c4b88fcd8176d104318259d91791877fed88fe91479d0') { throw 'libxposed dependency checksum mismatch.' }
 $ApiJar = "$BuildDir/deps/libxposed-api-102.jar"
 $archive = [System.IO.Compression.ZipFile]::OpenRead($aar)
 try { [System.IO.Compression.ZipFileExtensions]::ExtractToFile($archive.GetEntry('classes.jar'),$ApiJar,$true) } finally { $archive.Dispose() }
}
$sources = Get-ChildItem "$PSScriptRoot/src" -Recurse -Filter '*.java' | ForEach-Object FullName
Checked "$JavaHome/bin/javac.exe" (@('-encoding','UTF-8','-source','17','-target','17','-classpath',"$android;$ApiJar",'-d',"$BuildDir/classes") + $sources)
Checked "$JavaHome/bin/jar.exe" @("-J-Djava.io.tmpdir=$BuildDir",'cf',"$BuildDir/classes.jar",'-C',"$BuildDir/classes",'.')
Checked "$bt/d8.bat" @('--lib',$android,'--classpath',$ApiJar,'--min-api','29','--output',"$BuildDir/dex","$BuildDir/classes.jar")
Checked "$bt/aapt2.exe" @('link','-I',$android,'--manifest',"$PSScriptRoot/AndroidManifest.xml",'--version-code','13','--version-name','1.1.2','-o',"$BuildDir/unsigned.apk")
$zip = [System.IO.Compression.ZipFile]::Open("$BuildDir/unsigned.apk",[System.IO.Compression.ZipArchiveMode]::Update)
try {
 [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,"$BuildDir/dex/classes.dex",'classes.dex') | Out-Null
 Get-ChildItem "$PSScriptRoot/resources" -Recurse -File | ForEach-Object {
  $entry = $_.FullName.Substring((Join-Path $PSScriptRoot 'resources').Length + 1).Replace('\','/')
  [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$_.FullName,$entry) | Out-Null
 }
} finally { $zip.Dispose() }
Checked "$bt/zipalign.exe" @('-f','4',"$BuildDir/unsigned.apk","$BuildDir/aligned.apk")
if (!$Keystore) {
 $Keystore = "$BuildDir/debug.keystore"
 if (!(Test-Path -LiteralPath $Keystore)) { Checked "$JavaHome/bin/keytool.exe" @('-genkeypair','-keystore',$Keystore,'-storepass','android','-keypass','android','-alias','androiddebugkey','-dname','CN=NSG CC Repair Development','-keyalg','RSA','-validity','10000') }
 $signing = @('--ks-pass','pass:android','--key-pass','pass:android')
} else {
 $signing = @('--ks-pass',"env:$StorePasswordEnv",'--key-pass',"env:$KeyPasswordEnv")
}
$output = "$PSScriptRoot/dist/NSG-CC-Repair-1.1.2.apk"
Checked "$bt/apksigner.bat" (@('sign','--ks',$Keystore,'--ks-key-alias',$KeyAlias) + $signing + @('--out',$output,"$BuildDir/aligned.apk"))
Checked "$bt/apksigner.bat" @('verify','--verbose',$output)
Write-Output "Built $output"
