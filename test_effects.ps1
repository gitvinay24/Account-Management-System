$files = @(
  "D:\JAVA\BusinessManager\t_fade.pptx",
  "D:\JAVA\BusinessManager\t_wipe.pptx",
  "D:\JAVA\BusinessManager\t_fly.pptx",
  "D:\JAVA\BusinessManager\t_rise.pptx",
  "D:\JAVA\BusinessManager\t_zoom.pptx"
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
