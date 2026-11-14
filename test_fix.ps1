$files = @(
  "D:\JAVA\BusinessManager\v_canon.pptx",
  "D:\JAVA\BusinessManager\v_grpid.pptx",
  "D:\JAVA\BusinessManager\v_after.pptx",
  "D:\JAVA\BusinessManager\v_autostart.pptx",
  "D:\JAVA\BusinessManager\v_all.pptx"
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
