param(
    [string]$MysqlExe='',
    [string]$DbHost='127.0.0.1',
    [int]$DbPort=3306,
    [string]$AdminUser='root',
    [ValidatePattern('^[a-zA-Z][a-zA-Z0-9_]*$')][string]$Database='signup_demo'
)
$ErrorActionPreference='Stop'
$project=[System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\项目工程\前后端\backend'))
# 在询问密码或修改数据库之前检查并读取全部SQL文件。
$setupSql=@{}
foreach($sqlName in @('01_schema.sql','02_seed.sql','03_已有数据库启用注册.sql')) {
    $sqlPath=Join-Path $project ('sql\'+$sqlName)
    if(-not (Test-Path -LiteralPath $sqlPath -PathType Leaf)) {
        throw "缺少SQL文件：$sqlPath。请完整解压材料，保持快速启动与工程目录的相对位置后重新运行。"
    }
    $setupSql[$sqlName]=Get-Content -LiteralPath $sqlPath -Raw -Encoding UTF8
}
if([string]::IsNullOrWhiteSpace($MysqlExe)) {
    $found=Get-Command mysql.exe -ErrorAction SilentlyContinue
    if($found){$MysqlExe=$found.Source}
    else {
        $candidate=Get-ChildItem -Path "$env:ProgramFiles\MySQL\*\bin\mysql.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
        if($candidate){$MysqlExe=$candidate.FullName}
    }
}
if(-not (Test-Path -LiteralPath $MysqlExe -PathType Leaf)){throw '未找到mysql.exe。用-MysqlExe参数指定MySQL安装目录中bin\mysql.exe的完整路径。'}
$adminSecret=Read-Host 'MySQL管理员密码（输入不回显）' -AsSecureString
$workshopSecret=Read-Host 'workshop数据库密码（已有账号填原密码；新账号在此设置）' -AsSecureString
$adminCredential=New-Object System.Management.Automation.PSCredential($AdminUser,$adminSecret)
$workshopCredential=New-Object System.Management.Automation.PSCredential('workshop',$workshopSecret)
$previousPwd=$env:MYSQL_PWD
$previousEncoding=$OutputEncoding
function Invoke-SetupSql([string]$Sql,[string]$User,[string]$Password) {
    $env:MYSQL_PWD=$Password
    $result=$Sql | & $MysqlExe --no-defaults --protocol=TCP "--host=$DbHost" "--port=$DbPort" "--user=$User" --default-character-set=utf8mb4 --connect-timeout=5 --batch --skip-column-names
    if($LASTEXITCODE -ne 0){throw '数据库准备失败，请查看上方错误。已有workshop账号的密码不会被本脚本修改。'}
    return $result
}
try {
    $OutputEncoding=New-Object System.Text.UTF8Encoding($false)
    $adminPassword=$adminCredential.GetNetworkCredential().Password
    $workshopPassword=$workshopCredential.GetNetworkCredential().Password
    $quotedPassword=$workshopPassword.Replace("'","''")
    $prepare="SET SESSION sql_mode='NO_BACKSLASH_ESCAPES'; CREATE DATABASE IF NOT EXISTS ``$Database`` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; CREATE USER IF NOT EXISTS 'workshop'@'localhost' IDENTIFIED BY '$quotedPassword'; GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,REFERENCES ON ``$Database``.* TO 'workshop'@'localhost';"
    Invoke-SetupSql $prepare $AdminUser $adminPassword | Out-Null
    $schema=$setupSql['01_schema.sql']
    Invoke-SetupSql "USE ``$Database``; $schema" $AdminUser $adminPassword | Out-Null
    $idColumn=Invoke-SetupSql "USE ``$Database``; SHOW COLUMNS FROM users LIKE 'id';" $AdminUser $adminPassword
    if(($idColumn -join ' ') -notmatch 'auto_increment') {
        $upgrade=$setupSql['03_已有数据库启用注册.sql']
        Invoke-SetupSql "USE ``$Database``; $upgrade" $AdminUser $adminPassword | Out-Null
    }
    $seed=$setupSql['02_seed.sql']
    Invoke-SetupSql "USE ``$Database``; $seed" $AdminUser $adminPassword | Out-Null
    Invoke-SetupSql "USE ``$Database``; SELECT username FROM users WHERE username IN ('zhh','myh');" 'workshop' $workshopPassword
    Write-Host "准备完成：$DbHost`:$DbPort / $Database，数据库账号workshop。"
    Write-Host '接下来在IDEA打开项目工程/前后端/backend/pom.xml，填写上述连接信息与自己刚输入的workshop密码，再启动Java。'
    Write-Host '已有账号、密码和报名记录均保留；重复运行不会清空活动名额。'
} finally {
    $env:MYSQL_PWD=$previousPwd
    $OutputEncoding=$previousEncoding
    $adminCredential=$null;$workshopCredential=$null;$adminPassword=$null;$workshopPassword=$null;$quotedPassword=$null;$prepare=$null
}
