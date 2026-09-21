<#
  MySQL 定时备份脚本（备份的是 compose 里的 travel-mysql 容器）

  手动跑：
    powershell -ExecutionPolicy Bypass -File .\backup-mysql.ps1

  定时跑（Windows 任务计划程序）：
    创建基本任务 → 每天 03:00 → 启动程序
    程序：powershell.exe
    参数：-ExecutionPolicy Bypass -File "C:\Users\26859\Desktop\旅游app与网页简历\deploy\backup-mysql.ps1"

  备份文件放在脚本同级的 backups 目录，默认只保留最近 7 份。
#>
param(
    [string]$Container = "travel-mysql",
    [string]$User      = "root",
    [string]$Password  = $(if ($env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD } else { "2003" }),
    [string]$Database  = "travel",
    [int]$Keep         = 7
)

$backupDir = Join-Path $PSScriptRoot "backups"
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$file  = Join-Path $backupDir "travel-$stamp.sql"
$inContainer = "/tmp/travel-backup-$stamp.sql"

Write-Host "[备份] $Database -> $file"

# 让 mysqldump 在容器里直接写文件，再拷出来：这样不会因为 PowerShell 重定向改坏编码
docker exec $Container sh -c "mysqldump -u$User -p$Password --single-transaction --routines --databases $Database --result-file=$inContainer"
if ($LASTEXITCODE -ne 0) {
    Write-Error "mysqldump 执行失败：请确认容器在跑（docker ps）、密码正确。"
    exit 1
}

docker cp "${Container}:$inContainer" $file
docker exec $Container sh -c "rm -f $inContainer"

$size = (Get-Item $file).Length
if ($size -lt 1024) {
    Write-Warning "备份文件只有 $size 字节，可能不正常，请检查。"
} else {
    Write-Host ("[完成] 大小 {0:N1} KB" -f ($size / 1KB))
}

# 只保留最近 N 份
Get-ChildItem $backupDir -Filter "travel-*.sql" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -Skip $Keep |
    ForEach-Object {
        Write-Host ("[清理] 删除旧备份 " + $_.Name)
        Remove-Item $_.FullName -Force
    }
