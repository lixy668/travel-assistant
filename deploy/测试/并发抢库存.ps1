<#
  并发抢库存测试：验证「不超卖」

  原理：先把某天某房型库存设成 N，再让 M 个请求同时下单（M > N），
       正确的系统应该只有 N 个成功，其余全部返回“库存不足”。

  用法：
    powershell -ExecutionPolicy Bypass -File .\并发抢库存.ps1 -AdminToken "管理员token" -Token "用户token" -PlaceId 1 -Stock 5 -Requests 50
#>
param(
    [string]$BaseUrl   = "http://localhost:3200",
    [string]$AdminToken = "",
    [string]$Token      = "",
    [long]  $PlaceId    = 1,
    [string]$BizDate    = "2026-10-01",
    [string]$RoomType   = "标准大床房",
    [int]   $Stock      = 5,
    [int]   $Requests   = 50
)

[System.Net.ServicePointManager]::DefaultConnectionLimit = 1000
$handler = [System.Net.Http.HttpClientHandler]::new()
$handler.ServerCertificateCustomValidationCallback = { $true }
$client = [System.Net.Http.HttpClient]::new($handler)
$client.BaseAddress = [Uri]$BaseUrl
$client.Timeout = [TimeSpan]::FromSeconds(30)

if ([string]::IsNullOrWhiteSpace($AdminToken) -or [string]::IsNullOrWhiteSpace($Token)) {
    Write-Error "请先用 -AdminToken 和 -Token 传入管理员、普通用户的 token（登录接口返回的 data）"
    exit 1
}

# ---------- 1. 管理员设置库存 ----------
$adminClient = [System.Net.Http.HttpClient]::new($handler)
$adminClient.BaseAddress = [Uri]$BaseUrl
$adminClient.DefaultRequestHeaders.Add("Authorization", "Bearer $AdminToken")
$body = @{ placeId = $PlaceId; bizDate = $BizDate; roomType = $RoomType; total = $Stock } | ConvertTo-Json -Compress
$content = [System.Net.Http.StringContent]::new($body, [System.Text.Encoding]::UTF8, "application/json")
$setResp = $adminClient.PostAsync("/admin/stocks", $content).Result
Write-Host ("[1/3] 设置库存：$BizDate $RoomType = $Stock  -> HTTP " + [int]$setResp.StatusCode)

# ---------- 2. 并发下单 ----------
$client.DefaultRequestHeaders.Add("Authorization", "Bearer $Token")
Write-Host "[2/3] 并发发起 $Requests 个下单请求…"
$sw = [System.Diagnostics.Stopwatch]::StartNew()
$tasks = New-Object System.Collections.ArrayList
for ($i = 1; $i -le $Requests; $i++) {
    $payload = @{
        type       = "HOTEL"
        fromCity   = "并发测试酒店"
        toCity     = "上海"
        travelDate = $BizDate
        departTime = $BizDate
        seat       = "$RoomType × 1晚"
        price      = 100
        placeId    = $PlaceId
        bizDate    = $BizDate
        roomType   = $RoomType
        quantity   = 1
    } | ConvertTo-Json -Compress
    $req = [System.Net.Http.HttpRequestMessage]::new("POST", "/order/create")
    $req.Content = [System.Net.Http.StringContent]::new($payload, [System.Text.Encoding]::UTF8, "application/json")
    $req.Headers.Add("Idempotency-Key", "stock-test-" + [Guid]::NewGuid().ToString("N"))  # 每个请求独立幂等键
    $tasks.Add($client.SendAsync($req)) | Out-Null
}
[System.Threading.Tasks.Task]::WaitAll($tasks)
$sw.Stop()

$ok = 0; $soldOut = 0; $other = 0
foreach ($t in $tasks) {
    $resp = $t.Result
    $text = $resp.Content.ReadAsStringAsync().Result
    if ($text -match '"code":200') { $ok++ }
    elseif ($text -match '库存不足') { $soldOut++ }
    else { $other++ }
}

# ---------- 3. 对账 ----------
$check = $client.GetAsync("/travel/stock?placeId=$PlaceId&date=$BizDate").Result
$leftText = $check.Content.ReadAsStringAsync().Result

Write-Host ""
Write-Host "========== 结果 =========="
Write-Host ("库存设定     : $Stock")
Write-Host ("并发请求数   : $Requests")
Write-Host ("下单成功     : $ok        <- 应该正好等于库存数")
Write-Host ("提示库存不足 : $soldOut")
Write-Host ("其他响应     : $other")
Write-Host ("耗时         : {0:N2} 秒" -f $sw.Elapsed.TotalSeconds)
Write-Host ("库存现状     : $leftText")
Write-Host "=========================="
if ($ok -eq $Stock) {
    Write-Host "✅ 没有超卖：成功数正好等于库存数" -ForegroundColor Green
} else {
    Write-Host "❌ 结果异常：成功数 $ok ≠ 库存数 $Stock，请检查" -ForegroundColor Red
}
Write-Host "也可以去数据库复核：select sold,total from t_stock where place_id=$PlaceId and biz_date='$BizDate' and room_type='$RoomType';"
