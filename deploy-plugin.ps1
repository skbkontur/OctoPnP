$ErrorActionPreference = "Stop";

$BuildOutputDir = ".\src\target"
$TeamCityDataDir = "d:\temp\team-city"

if (-NOT ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator"))
{    
    throw "This script needs to be run As Admin"
}

Stop-Service TeamCity
Stop-Service TCBuildAgent

Copy-Item "$BuildOutputDir\octo-pnp.zip" "$TeamCityDataDir\plugins\octo-pnp.zip" -Force

Start-Service TeamCity
Start-Service TCBuildAgent

