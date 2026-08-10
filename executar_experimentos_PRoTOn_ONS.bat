@echo off
setlocal EnableExtensions EnableDelayedExpansion

REM ============================================================
REM PRoTOn_ONS - Executor automatizado de experimentos
REM Uso:
REM   executar_experimentos_PRoTOn_ONS.bat A
REM       Fase A: D1/D2/D3, load 50, seeds 1..30 (90 execucoes)
REM
REM   executar_experimentos_PRoTOn_ONS.bat B
REM       Fase B: D1/D2/D3, loads 10..100, seeds 1..50
REM       (1500 execucoes)
REM
REM O script AUTOMATIZA:
REM   - combinacoes de XML / load / seed
REM   - execucao do Ant
REM   - salvamento do console em .txt
REM   - registro de progresso (status tecnico)
REM
REM O script NAO AUTOMATIZA:
REM   - validacao cientifica dos resultados
REM   - extracao das metricas para a planilha
REM   - conferencia de outliers/inconsistencias
REM ============================================================

set "ALGORITMO=PRoTOn_ONS"
set "TAG_OBRIGATORIA=proton-ons-experiment-v1"
set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"

if "%~1"=="" goto :USO
if /I "%~1"=="A" (
    set "FASE=FASE_A_VALIDACAO"
    set "LOADS=50"
    set "SEED_MAX=30"
) else if /I "%~1"=="B" (
    set "FASE=FASE_B_TESTES"
    set "LOADS=10 20 30 40 50 60 70 80 90 100"
    set "SEED_MAX=50"
) else (
    goto :USO
)

echo.
echo ============================================================
echo PRoTOn_ONS - %FASE%
echo ============================================================
echo Projeto: %ROOT%
echo.

REM --- Preflight: Git
where git >nul 2>&1
if errorlevel 1 (
    echo [ERRO] Git nao encontrado no PATH.
    exit /b 1
)

pushd "%ROOT%" >nul

for /f "delims=" %%H in ('git rev-parse HEAD 2^>nul') do set "HEAD_HASH=%%H"
for /f "delims=" %%H in ('git rev-list -n 1 %TAG_OBRIGATORIA% 2^>nul') do set "TAG_HASH=%%H"

if not defined HEAD_HASH (
    echo [ERRO] Esta pasta nao parece ser um repositorio Git valido.
    popd
    exit /b 1
)

if not defined TAG_HASH (
    echo [ERRO] A tag %TAG_OBRIGATORIA% nao foi encontrada.
    echo Execute: git fetch --all --tags
    popd
    exit /b 1
)

if /I not "%HEAD_HASH%"=="%TAG_HASH%" (
    echo [ERRO] O codigo atual NAO corresponde a tag %TAG_OBRIGATORIA%.
    echo HEAD atual: %HEAD_HASH%
    echo Tag       : %TAG_HASH%
    echo.
    echo Execute:
    echo   git checkout %TAG_OBRIGATORIA%
    popd
    exit /b 1
)

REM --- Preflight: XMLs
for %%D in (D1 D2 D3) do (
    if not exist "%ROOT%\test\MINNESOTA_ONS_%%D.xml" (
        echo [ERRO] Arquivo ausente: test\MINNESOTA_ONS_%%D.xml
        popd
        exit /b 1
    )
)

REM --- Localiza Ant
set "ANT_EXEC="
where ant >nul 2>&1
if not errorlevel 1 set "ANT_EXEC=ant"

if not defined ANT_EXEC if defined ANT_HOME (
    if exist "%ANT_HOME%\bin\ant.bat" set "ANT_EXEC=%ANT_HOME%\bin\ant.bat"
)

if not defined ANT_EXEC if defined NETBEANS_HOME (
    if exist "%NETBEANS_HOME%\extide\ant\bin\ant.bat" set "ANT_EXEC=%NETBEANS_HOME%\extide\ant\bin\ant.bat"
)

if not defined ANT_EXEC (
    echo [ERRO] Apache Ant nao foi encontrado.
    echo.
    echo Opcoes:
    echo  1. Instalar o Apache Ant e adiciona-lo ao PATH; ou
    echo  2. Definir ANT_HOME apontando para a instalacao do Ant; ou
    echo  3. Definir NETBEANS_HOME para a pasta do NetBeans, se ela contiver
    echo     extide\ant\bin\ant.bat.
    popd
    exit /b 1
)

set "RESULT_ROOT=%ROOT%\resultados_executortestes\%FASE%\%ALGORITMO%"
if not exist "%RESULT_ROOT%" mkdir "%RESULT_ROOT%"

set "PROGRESSO=%RESULT_ROOT%\progresso_execucoes.csv"
if not exist "%PROGRESSO%" (
    >"%PROGRESSO%" echo algoritmo,fase,desastre,load,seed,status,arquivo_output
)

set "MANIFESTO=%RESULT_ROOT%\manifesto_ambiente.txt"
if not exist "%MANIFESTO%" (
    >"%MANIFESTO%" echo PRoTOn_ONS - Manifesto de ambiente
    >>"%MANIFESTO%" echo ==================================
    >>"%MANIFESTO%" echo Fase: %FASE%
    >>"%MANIFESTO%" echo Tag obrigatoria: %TAG_OBRIGATORIA%
    >>"%MANIFESTO%" echo Git hash: %HEAD_HASH%
    >>"%MANIFESTO%" echo Inicio: %DATE% %TIME%
    >>"%MANIFESTO%" echo.
    >>"%MANIFESTO%" echo [git --version]
    git --version >>"%MANIFESTO%" 2>&1
    >>"%MANIFESTO%" echo.
    >>"%MANIFESTO%" echo [java -version]
    java -version >>"%MANIFESTO%" 2>&1
    >>"%MANIFESTO%" echo.
    >>"%MANIFESTO%" echo [javac -version]
    javac -version >>"%MANIFESTO%" 2>&1
    >>"%MANIFESTO%" echo.
    >>"%MANIFESTO%" echo [ant -version]
    if /I "%ANT_EXEC%"=="ant" (
        ant -version >>"%MANIFESTO%" 2>&1
    ) else (
        call "%ANT_EXEC%" -version >>"%MANIFESTO%" 2>&1
    )
)

set /a TOTAL=0
set /a EXECUTADOS=0
set /a PULADOS=0
set /a ERROS=0

for %%D in (D1 D2 D3) do (
    for %%L in (%LOADS%) do (
        for /L %%S in (1,1,%SEED_MAX%) do (
            set /a TOTAL+=1

            set "SEED_PAD=00%%S"
            set "SEED_PAD=!SEED_PAD:~-3!"

            set "DIR_OUT=%RESULT_ROOT%\%%D\L%%L"
            if not exist "!DIR_OUT!" mkdir "!DIR_OUT!"

            set "OUTFILE=!DIR_OUT!\%ALGORITMO%_%%D_L%%L_S!SEED_PAD!.txt"

            if exist "!OUTFILE!" (
                echo [PULADO] %%D load=%%L seed=%%S - output ja existe
                set /a PULADOS+=1
            ) else (
                set "XML=test\MINNESOTA_ONS_%%D.xml"
                set "ARGS=!XML! %%S %%S %%L %%L 1"

                echo.
                echo ------------------------------------------------------------
                echo [EXECUTANDO] %%D  load=%%L  seed=%%S
                echo Output: !OUTFILE!
                echo ------------------------------------------------------------

                if /I "%ANT_EXEC%"=="ant" (
                    call ant -f "%ROOT%" -Dnb.internal.action.name=run -Dapplication.args="!ARGS!" run >"!OUTFILE!" 2>&1
                ) else (
                    call "%ANT_EXEC%" -f "%ROOT%" -Dnb.internal.action.name=run -Dapplication.args="!ARGS!" run >"!OUTFILE!" 2>&1
                )

                set "RC=!ERRORLEVEL!"
                if "!RC!"=="0" (
                    echo [OK] %%D load=%%L seed=%%S
                    >>"%PROGRESSO%" echo %ALGORITMO%,%FASE%,%%D,%%L,%%S,OK,!OUTFILE!
                ) else (
                    echo [ERRO] %%D load=%%L seed=%%S - codigo !RC!
                    >>"%PROGRESSO%" echo %ALGORITMO%,%FASE%,%%D,%%L,%%S,ERRO,!OUTFILE!
                    set /a ERROS+=1
                )
                set /a EXECUTADOS+=1
            )
        )
    )
)

echo.
echo ============================================================
echo FIM DA EXECUCAO
echo ============================================================
echo Combinacoes previstas : %TOTAL%
echo Executadas nesta rodada: %EXECUTADOS%
echo Puladas (ja existiam)  : %PULADOS%
echo Erros tecnicos         : %ERROS%
echo.
echo Resultados:
echo %RESULT_ROOT%
echo.
echo IMPORTANTE:
echo O status OK significa apenas que o processo terminou sem erro tecnico.
echo O executor ainda deve conferir os outputs e preencher a planilha
echo com as metricas cientificas previstas no protocolo.
echo ============================================================

popd
exit /b 0

:USO
echo.
echo Uso:
echo   %~nx0 A
echo       Fase A: 3 desastres x load 50 x seeds 1..30 = 90 testes
echo.
echo   %~nx0 B
echo       Fase B: 3 desastres x 10 loads x seeds 1..50 = 1500 testes
echo.
exit /b 2
