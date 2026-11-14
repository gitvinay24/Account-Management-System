try {
  $pp = New-Object -ComObject PowerPoint.Application
  Write-Host ("PowerPoint COM OK, version: " + $pp.Version)
  $src = "D:\JAVA\BusinessManager\ClaimChain_Modern_Animated_v2.pptx"
  $png = "D:\JAVA\BusinessManager\render_check.png"
  if (Test-Path $png) { Remove-Item $png }
  $pres = $pp.Presentations.Open($src, $true, $false, $false)  # readonly, no window
  Write-Host ("Opened OK. Slides: " + $pres.Slides.Count)
  $pres.Slides.Item(1).Export($png, "PNG", 1280, 720)
  Write-Host "Slide 1 exported."
  $pres.Slides.Item(4).Export("D:\JAVA\BusinessManager\render_check_s4.png", "PNG", 1280, 720)
  Write-Host "Slide 4 exported."
  $pres.Close()
  $pp.Quit()
  Write-Host "VALIDATION PASSED - PowerPoint opened the file without repair."
} catch {
  Write-Host ("VALIDATION FAILED: " + $_.Exception.Message)
  try { $pres.Close() } catch {}
  try { $pp.Quit() } catch {}
}
