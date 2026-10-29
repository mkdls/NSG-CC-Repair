param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
if (!$JavaHome) { throw 'Set JAVA_HOME or pass -JavaHome.' }
$out = "$PSScriptRoot/.build/tests"
New-Item -ItemType Directory -Force $out | Out-Null
& "$JavaHome/bin/javac.exe" -encoding UTF-8 -d $out "$PSScriptRoot/src/com/nsgcc/PassiveRefresh.java" "$PSScriptRoot/tests/com/nsgcc/PassiveRefreshTest.java" "$PSScriptRoot/src/com/nsgcc/CursorPolicy.java" "$PSScriptRoot/tests/com/nsgcc/CursorPolicyTest.java" "$PSScriptRoot/src/com/nsgcc/RrcDecodePlan.java" "$PSScriptRoot/src/com/nsgcc/RelayLogIndex.java" "$PSScriptRoot/src/com/nsgcc/RrcCarrierFallback.java" "$PSScriptRoot/tests/com/nsgcc/RrcDecodePlanTest.java" "$PSScriptRoot/tests/com/nsgcc/CoreTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
foreach ($test in @('com.nsgcc.PassiveRefreshTest','com.nsgcc.RrcDecodePlanTest','com.nsgcc.CoreTest','com.nsgcc.CursorPolicyTest')) {
 & "$JavaHome/bin/java.exe" -cp $out $test
 if ($LASTEXITCODE -ne 0) { throw "$test failed." }
}
