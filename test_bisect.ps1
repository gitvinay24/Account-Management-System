$files = @(
  "D:\JAVA\BusinessManager\bisect_v1.pptx",
  "D:\JAVA\BusinessManager\bisect_trans.pptx",
  "D:\JAVA\BusinessManager\bisect_trans_p14.pptx"
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
