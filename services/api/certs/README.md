# Local corporate CA certificates

This directory holds the corporate TLS-inspection root/issuing CA certificates
exported from the Windows machine's trust store (`Cert:\LocalMachine\Root`),
so the Maven build stage in `../Dockerfile` can reach Maven Central through
this network's SSL-inspecting proxy.

These files are machine/network-specific, contain no private key material,
and are `.gitignore`d — never commit them. On a different network or CI
runner without this proxy, the Dockerfile's cert-import step is skipped
automatically if this directory is empty.

To regenerate on a Windows machine behind the same proxy:

```powershell
Get-ChildItem -Path Cert:\LocalMachine\Root | Where-Object { $_.Subject -match 'Emirates' } | ForEach-Object {
  $name = ($_.Subject -replace '[^a-zA-Z0-9]', '_')
  [System.IO.File]::WriteAllBytes("services\api\certs\$name.crt", $_.Export('Cert'))
}
```
