$ErrorActionPreference='Stop'
function Read-Default([string]$Prompt,[string]$Default){
    $value=Read-Host "$Prompt [$Default]"
    if([string]::IsNullOrWhiteSpace($value)){return $Default}
    return $value.Trim()
}
$names=@('DB_HOST','DB_PORT','DB_NAME','DB_USER','DB_PASSWORD','DB_INIT','SERVER_PORT')
$previous=@{}
foreach($name in $names){$previous[$name]=[Environment]::GetEnvironmentVariable($name,'Process')}
try{
    if(-not (Get-Command java -ErrorAction SilentlyContinue)){throw '未找到Java。安装JDK21后重新打开窗口。'}
    $env:DB_HOST=Read-Default 'MySQL主机' '127.0.0.1'
    $env:DB_PORT=Read-Default 'MySQL端口' '3306'
    $env:DB_NAME=Read-Default '数据库名' 'signup_demo'
    $env:DB_USER=Read-Default '数据库账号' 'workshop'
    $secure=Read-Host '数据库密码' -AsSecureString
    $credential=New-Object System.Management.Automation.PSCredential('workshop',$secure)
    $env:DB_PASSWORD=$credential.GetNetworkCredential().Password
    $env:SERVER_PORT=Read-Default '网页端口' '8080'
    $env:DB_INIT='never'
    Write-Host "启动后打开 http://127.0.0.1:$($env:SERVER_PORT)/ ，停止时按Ctrl+C。"
    & java -jar (Join-Path $PSScriptRoot 'signup-classroom-1.0.0.jar')
    if($LASTEXITCODE -ne 0){throw '启动失败，请查看上方日志。'}
}finally{
    foreach($name in $names){[Environment]::SetEnvironmentVariable($name,$previous[$name],'Process')}
    $credential=$null;$secure=$null
}
