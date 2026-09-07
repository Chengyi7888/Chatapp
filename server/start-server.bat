@echo off
setlocal
cd /d "%~dp0"
if not defined PORT set PORT=8899
if not defined UPDATE_PORT set UPDATE_PORT=8900
node server.js
