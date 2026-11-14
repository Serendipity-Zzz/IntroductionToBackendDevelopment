param(
    [string]$BaseUrl='http://127.0.0.1:8080',
    [long]$EventId=1,
    [string]$Username='myh',
    [string]$Password='demo123',
    [switch]$CheckAddedEndpoint
)
$ErrorActionPreference='Stop'
$BaseUrl=$BaseUrl.TrimEnd('/')
function Assert-That([bool]$Condition,[string]$Message){
    if(-not $Condition){throw $Message}
    Write-Host "PASS $Message"
}
$loginBody=@{username=$Username;password=$Password}|ConvertTo-Json -Compress
$login=Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -ContentType 'application/json' -Body $loginBody
$headers=@{Authorization="Bearer $($login.token)"}
Assert-That (-not [string]::IsNullOrWhiteSpace($login.token)) 'login returned a token'
$event=Invoke-RestMethod -Uri "$BaseUrl/api/events/$EventId" -Headers $headers
Assert-That ($event.id -eq $EventId) 'event ID matches'
try{
    $registered=Invoke-RestMethod -Uri "$BaseUrl/api/events/$EventId/registrations" -Headers $headers -Method Post -ContentType 'application/json' -Body '{}'
    Write-Host 'Registration created (201).'
}catch{
    $requestFailure=$_
    $errorBody=$null
    $errorText=$requestFailure.ErrorDetails.Message
    # Windows PowerShell 5可能没有ErrorDetails，正文仍在响应流里。
    if([string]::IsNullOrWhiteSpace($errorText) -and $null -ne $requestFailure.Exception.Response){
        $response=$requestFailure.Exception.Response
        if($response.PSObject.Methods.Name -contains 'GetResponseStream'){
            $reader=New-Object System.IO.StreamReader($response.GetResponseStream())
            try{$errorText=$reader.ReadToEnd()}finally{$reader.Dispose()}
        }elseif($null -ne $response.Content){
            $errorText=$response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        }
    }
    try{$errorBody=$errorText|ConvertFrom-Json}catch{}
    if(($null -ne $requestFailure.Exception.Response) -and ([int]$requestFailure.Exception.Response.StatusCode -eq 409) -and ($errorBody.code -eq 'ALREADY_REGISTERED')){
        Write-Host 'Already registered (409); checking the existing record.'
        $registered=Invoke-RestMethod -Uri "$BaseUrl/api/events/$EventId/my-registration" -Headers $headers
    }else{throw $requestFailure}
}
Assert-That ($registered.userId -eq $login.user.id) 'registration belongs to this user'
Assert-That ($registered.eventId -eq $EventId) 'registration belongs to this event'
$result=Invoke-RestMethod -Uri "$BaseUrl/api/registrations/$($registered.id)" -Headers $headers
Assert-That ($result.id -eq $registered.id -and $result.userId -eq $login.user.id) 'query returned the same registration'
$mine=Invoke-RestMethod -Uri "$BaseUrl/api/events/$EventId/my-registration" -Headers $headers
Assert-That ($mine.id -eq $registered.id) 'new endpoint returned the same registration'
if($CheckAddedEndpoint){
    $added=Invoke-RestMethod -Uri "$BaseUrl/api/events/$EventId/registration-status" -Headers $headers
    Assert-That ($added.id -eq $registered.id -and $added.userId -eq $login.user.id -and $added.eventId -eq $EventId) 'added endpoint returned the same owned registration'
}
Write-Host 'All checks passed.'

