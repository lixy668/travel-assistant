<#
  简单压测脚本（不用装 JMeter，PowerShell 直接跑）

  用法：
    powershell -ExecutionPolicy Bypass -File .\压测脚本.ps1
    powershell -ExecutionPolicy Bypass -File .\压测脚本.ps1 -BaseUrl "https://localhost" -Total 500

  想测需要登录的接口，先登录拿 token，再传 -Token：
    powershell -ExecutionPolicy Bypass -File .\压测脚本.ps1 -Path "/order/my" -Token "你的token"
#>
param(
    [string]$BaseUrl = "http://localhost:3200",
    [string]$Path = "/travel/hello",
    [int]$Total = 200,
    [int]$Concurrency = 20,
    [string]$Token = ""
)

# .NET Framework 默认只允许 2 个并发连接，压测前必须放开
[System.Net.ServicePointManager]::DefaultConnectionLimit = 1000
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12

$handler = [System.Net.Http.HttpClientHandler]::new()
$handler.ServerCertificateCustomValidationCallback = { $true }   # 自签名证书也放行
$client = [System.Net.Http.HttpClient]::new($handler)
$client.BaseAddress = [Uri]$BaseUrl
$client.Timeout = [TimeSpan]::FromSeconds(30)
if ($Token) { $client.DefaultRequestHeaders.Add("Authorization", "Bearer $Token") }

Write-Host "压测目标：$BaseUrl$Path   总数 $Total，并发 $Concurrency"

$sw = [System.Diagnostics.Stopwatch]::StartNew()
$ok = 0; $fail = 0; $limited = 0; $other = 0
$pending = New-Object System.Collections.ArrayList

for ($i = 0; $i -lt $Total; $i++) {
    $pending.Add($client.GetAsync($Path)) | Out-Null
    if ($pending.Count -ge $Concurrency) {
        foreach ($t in $pending) { $t.Wait() | Out-Null }
        foreach ($t in $pending) {
            $code = [int]$t.Result.StatusCode
            if ($code -eq 200) { $ok++ }
            elseif ($code -eq 429) { $limited++ }
            elseif ($code -ge 500) { $fail++ }
            else { $other++ }
        }
        $pending.Clear()
        Write-Host ("  已完成 $i / $Total") -NoNewline; Write-Host "`r" -NoNewline
    }
}
foreach ($t in $pending) {
    $t.Wait() | Out-Null
    $code = [int]$t.Result.StatusCode
    if ($code -eq 200) { $ok++ } elseif ($code -eq 429) { $limited++ } elseif ($code -ge 500) { $fail++ } else { $other++ }
}
$sw.Stop()

$seconds = [Math]::Max($sw.Elapsed.TotalSeconds, 0.001)
Write-Host ""
Write-Host "========== 压测结果 =========="
Write-Host ("总请求     : $Total")
Write-Host ("成功 200   : $ok")
Write-Host ("被限流 429 : $limited   （限流生效的证明）")
Write-Host ("服务端 5xx : $fail")
Write-Host ("其他状态   : $other")
Write-Host ("总耗时     : {0:N2} 秒" -f $seconds)
Write-Host ("平均 QPS   : {0:N1}" -f ($Total / $seconds))
Write-Host ("平均延迟   : {0:N0} ms" -f ($sw.Elapsed.TotalMilliseconds / $Total))
Write-Host "=============================="
Write-Host "提示：想要 P95、响应时间曲线、并发折线图，用 JMeter 打开同样的接口即可。"
