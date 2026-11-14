$files = @(
  "D:\JAVA\BusinessManager\s1_one.pptx",
  "D:\JAVA\BusinessManager\s2_three.pptx",
  "D:\JAVA\BusinessManager\s3_canon3.pptx"
)
try {
  $pp = New-Object -ComObject PowerPoint.Application
  foreach ($f in $files) {
    try {
      $p = $pp.Presentations.Open($f, $true, $false, $false)
      Write-Host "OK: $f slides=$($p.Slides.Count)"
      $p.Close()
    } catch {
      Write-Host "FAIL: $f -- $($_.Exception.Message)"
    }
  }
  $pp.Quit()
} catch {
  Write-Host "COM error: $($_.Exception.Message)"
}
