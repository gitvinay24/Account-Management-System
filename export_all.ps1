try {
  $pp = New-Object -ComObject PowerPoint.Application
  $src = "D:\JAVA\BusinessManager\ClaimChain_Modern_Animated_v2.pptx"
  $pres = $pp.Presentations.Open($src, $true, $false, $false)
  New-Item -ItemType Directory -Force -Path "D:\JAVA\BusinessManager\renders" | Out-Null
  for ($i = 1; $i -le 9; $i++) {
    $out = "D:\JAVA\BusinessManager\renders\final_s$i.png"
    $pres.Slides.Item($i).Export($out, "PNG", 1280, 720)
  }
  Write-Host "Exported all 9 slides."
  $pres.Close()
  $pp.Quit()
} catch {
  Write-Host "ERROR: $($_.Exception.Message)"
}
