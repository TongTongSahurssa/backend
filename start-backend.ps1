$ErrorActionPreference = "Stop"

$senderEmail = "liewyikpui@gmail.com"
$appPassword = Read-Host "Enter the Google App Password for $senderEmail" -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($appPassword)

try {
    $env:SMTP_HOST = "smtp.gmail.com"
    $env:SMTP_PORT = "587"
    $env:SMTP_USERNAME = $senderEmail
    $env:SMTP_PASSWORD = (
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    ).Replace(" ", "")
    $env:MAIL_FROM = $senderEmail
    $env:MAIL_DEVELOPMENT_MODE = "false"

    $jwtSecret = [Environment]::GetEnvironmentVariable("JWT_SECRET", "User")
    if ([string]::IsNullOrWhiteSpace($jwtSecret)) {
        $randomBytes = New-Object byte[] 48
        $randomGenerator = [Security.Cryptography.RandomNumberGenerator]::Create()
        try {
            $randomGenerator.GetBytes($randomBytes)
        }
        finally {
            $randomGenerator.Dispose()
        }
        $jwtSecret = [Convert]::ToBase64String($randomBytes)
        [Environment]::SetEnvironmentVariable("JWT_SECRET", $jwtSecret, "User")
    }
    $env:JWT_SECRET = $jwtSecret

    Write-Host "Starting the backend with Gmail verification enabled..."
    & mvn spring-boot:run
}
finally {
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }

    Remove-Item Env:SMTP_PASSWORD -ErrorAction SilentlyContinue
}
