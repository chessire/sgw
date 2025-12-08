# ===================================================================
# Money Inclusion Game - Full Gameplay Test
# - User API (check, create, login, nickname, info, logout)
# - Tutorial (6 rounds) complete play
# - Competition (12 rounds) complete play
# - Results, Achievements, Rankings check
# ===================================================================

param(
    [string]$Server = "http://localhost:8080/api",
    # Use numeric UID for User API compatibility (mbrSno = Long)
    # Format: timestamp-based number (e.g., 20251207055320)
    [string]$Uid = (Get-Date -Format "yyyyMMddHHmmss"),
    [switch]$SkipTutorial,
    [switch]$SkipCompetition,
    [switch]$Verbose
)

# ===================================================================
# Configuration
# ===================================================================
$headers = @{
    "uid" = $Uid
    "Content-Type" = "application/json"
}

$script:totalTests = 0
$script:passedTests = 0
$script:failedTests = 0

# ===================================================================
# Utility Functions
# ===================================================================
function Write-Section($title) {
    Write-Host ""
    Write-Host ("=" * 60) -ForegroundColor Cyan
    Write-Host " $title" -ForegroundColor Cyan
    Write-Host ("=" * 60) -ForegroundColor Cyan
}

function Write-SubSection($title) {
    Write-Host "`n--- $title ---" -ForegroundColor Yellow
}

function Write-Success($message) {
    $script:totalTests++
    $script:passedTests++
    Write-Host "  [PASS] $message" -ForegroundColor Green
}

function Write-Fail($message) {
    $script:totalTests++
    $script:failedTests++
    Write-Host "  [FAIL] $message" -ForegroundColor Red
}

function Write-Info($message) {
    Write-Host "  $message" -ForegroundColor White
}

function Write-Detail($message) {
    if ($Verbose) {
        Write-Host "  $message" -ForegroundColor Gray
    }
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Endpoint,
        [object]$Body = $null,
        [switch]$SuppressError
    )
    
    $uri = "$Server$Endpoint"
    
    try {
        $params = @{
            Uri = $uri
            Method = $Method
            Headers = $headers
            ContentType = "application/json"
        }
        
        if ($Body) {
            $params.Body = ($Body | ConvertTo-Json -Depth 10)
        }
        
        $response = Invoke-RestMethod @params
        return @{ Success = $true; Data = $response }
    }
    catch {
        $errorMsg = $_.Exception.Message
        $errorDetails = $null
        
        if ($_.ErrorDetails.Message) {
            try {
                $errorDetails = $_.ErrorDetails.Message | ConvertFrom-Json
            } catch {}
        }
        
        if (-not $SuppressError) {
            Write-Host "  [ERROR] $errorMsg" -ForegroundColor Red
            if ($errorDetails) {
                Write-Host "  Code: $($errorDetails.code), Message: $($errorDetails.message)" -ForegroundColor Yellow
            }
        }
        
        return @{ Success = $false; Error = $errorMsg; ErrorDetails = $errorDetails }
    }
}

function Format-Won($amount) {
    return "{0:N0} won" -f $amount
}

function Format-Percent($rate) {
    if ($rate -is [string]) {
        return $rate
    }
    return "{0:P1}" -f $rate
}

# ===================================================================
# Main Test Start
# ===================================================================
Write-Section "Money Inclusion Game - Full Test"
Write-Host "`n  Server: $Server" -ForegroundColor White
Write-Host "  UID: $Uid" -ForegroundColor White
Write-Host "  Time: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor White

# ===================================================================
# 1. Server Health Check
# ===================================================================
Write-SubSection "1. Server Health Check"

# Health endpoint is at /api/health, but Server already includes /api
# So we call just /health or check the base URL structure
$healthEndpoint = "/health"
# If server URL ends with /api, adjust the endpoint
if ($Server -match "/api$") {
    $healthEndpoint = "/health"
}

$health = Invoke-Api -Method GET -Endpoint $healthEndpoint -SuppressError
if ($health.Success) {
    Write-Success "Server connection OK"
} else {
    # Try alternative health check
    Write-Info "Trying validate API instead..."
    $validateCheck = Invoke-Api -Method POST -Endpoint "/v1/validate" -Body @{ dataFileMD5 = "test" } -SuppressError
    if ($validateCheck.Success) {
        Write-Success "Server connection OK (via validate API)"
    } else {
        Write-Fail "Server connection failed - Check if server is running"
        Write-Host "`nTest aborted." -ForegroundColor Red
        exit 1
    }
}

# ===================================================================
# 2. User API Tests
# ===================================================================
Write-Section "User API Tests"

# 2.1 Check User
Write-SubSection "2.1 Check User (GET /v1/user/check-user)"
$checkUser = Invoke-Api -Method GET -Endpoint "/v1/user/check-user"
if ($checkUser.Success) {
    $userExists = $checkUser.Data.data.exists
    Write-Success "Check User API OK"
    Write-Info "User exists: $userExists"
    if ($userExists -and $checkUser.Data.data.user) {
        Write-Info "Nickname: $($checkUser.Data.data.user.ninamNm)"
    }
} else {
    Write-Fail "Check User API failed"
}

# 2.2 Create User (if not exists) - 형용사, NPC 모두 랜덤 생성
Write-SubSection "2.2 Create User (POST /v1/user/create-user)"
if (-not $userExists) {
    $createUser = Invoke-Api -Method POST -Endpoint "/v1/user/create-user" -Body @{}
    if ($createUser.Success -and $createUser.Data.data.success) {
        Write-Success "User created"
        Write-Info "Nickname: $($createUser.Data.data.user.NINAM_NM)"
        Write-Info "NPC No: $($createUser.Data.data.user.NPC_NO)"
    } else {
        Write-Info "User creation skipped or failed (may already exist)"
    }
} else {
    Write-Info "User already exists, skipping creation"
}

# 2.3 Login
Write-SubSection "2.3 Login (POST /v1/user/login)"
$login = Invoke-Api -Method POST -Endpoint "/v1/user/login" -Body @{}
if ($login.Success -and $login.Data.data.success) {
    Write-Success "Login successful"
    Write-Info "Login time: $($login.Data.data.loginTime)"
} else {
    Write-Fail "Login failed"
}

# 2.4 Check Login Status
Write-SubSection "2.4 Check Login Status (GET /v1/user/check-login)"
$checkLogin = Invoke-Api -Method GET -Endpoint "/v1/user/check-login"
if ($checkLogin.Success) {
    Write-Success "Check Login API OK"
    Write-Info "Is logged in: $($checkLogin.Data.data.isLoggedIn)"
} else {
    Write-Fail "Check Login API failed"
}

# 2.5 Get User Info
Write-SubSection "2.5 Get User Info (GET /v1/user/info)"
$userInfo = Invoke-Api -Method GET -Endpoint "/v1/user/info"
if ($userInfo.Success -and $userInfo.Data.data) {
    Write-Success "User Info retrieved"
    Write-Info "MBR_SNO: $($userInfo.Data.data.mbrSno)"
    Write-Info "Nickname: $($userInfo.Data.data.ninamNm)"
} else {
    Write-Info "User Info not available (user may not exist)"
}

# 2.6 Change Nickname
Write-SubSection "2.6 Change Nickname (POST /v1/user/change-nickname)"
$changeNickname = Invoke-Api -Method POST -Endpoint "/v1/user/change-nickname" -Body @{
    npcNo = 2  # Chaeumi
}
if ($changeNickname.Success -and $changeNickname.Data.data.success) {
    Write-Success "Nickname changed"
    Write-Info "New nickname: $($changeNickname.Data.data.newNickname)"
} else {
    Write-Info "Nickname change skipped or failed"
}

# 2.7 Refresh Session
Write-SubSection "2.7 Refresh Session (POST /v1/user/refresh-session)"
$refreshSession = Invoke-Api -Method POST -Endpoint "/v1/user/refresh-session" -Body @{}
if ($refreshSession.Success) {
    Write-Success "Session refreshed"
} else {
    Write-Info "Session refresh skipped (may not be logged in)"
}

# ===================================================================
# 3. Validate API Check
# ===================================================================
Write-SubSection "3. Validate API Check"

$validateBody = @{ dataFileMD5 = "test-md5" }
$validate = Invoke-Api -Method POST -Endpoint "/v1/validate" -Body $validateBody

if ($validate.Success) {
    Write-Success "Validate API OK"
    Write-Info "Tutorial: $($validate.Data.data.existingGame.tutorial)"
    Write-Info "Competition: $($validate.Data.data.existingGame.competition)"
} else {
    Write-Fail "Validate API failed"
}

# ===================================================================
# 4. Tutorial Full Play
# ===================================================================
if (-not $SkipTutorial) {
    Write-Section "Tutorial (6 Rounds) Full Play"
    
    # 4.1 Reset existing game
    Write-SubSection "4.1 Reset Existing Game"
    $reset = Invoke-Api -Method DELETE -Endpoint "/v1/tutorial/reset" -SuppressError
    Write-Info "Reset complete"
    
    # 4.2 Complete Opening Story
    Write-SubSection "4.2 Complete Opening Story"
    $opening = Invoke-Api -Method POST -Endpoint "/v1/tutorial/complete-opening-story" -Body @{}
    if ($opening.Success -and $opening.Data.data.completed) {
        Write-Success "Opening story completed"
    } else {
        Write-Fail "Opening story failed"
    }
    
    # 4.3 Submit Propensity Test
    Write-SubSection "4.3 Submit Propensity Test"
    $propensityTest = Invoke-Api -Method POST -Endpoint "/v1/tutorial/submit-propensity-test" -Body @{
        answers = @(3, 1, 2, 0, 4, 2, 1, 3)
    }
    if ($propensityTest.Success -and $propensityTest.Data.data.submitted) {
        Write-Success "Propensity test submitted"
    } else {
        Write-Fail "Propensity test failed"
    }
    
    # 4.4 Save Propensity Result
    Write-SubSection "4.4 Save Propensity Result"
    $propensityResult = Invoke-Api -Method POST -Endpoint "/v1/tutorial/save-propensity-result" -Body @{
        propensityType = "BALANCED"
    }
    if ($propensityResult.Success -and $propensityResult.Data.data.saved) {
        Write-Success "Propensity result saved (BALANCED)"
    } else {
        Write-Fail "Propensity result save failed"
    }
    
    # 4.5 Assign NPC
    Write-SubSection "4.5 Assign NPC"
    $npcAssign = Invoke-Api -Method POST -Endpoint "/v1/tutorial/assign-npc" -Body @{
        npcType = "POYONGI"
    }
    if ($npcAssign.Success -and $npcAssign.Data.data.assigned) {
        Write-Success "NPC assigned (POYONGI)"
    } else {
        Write-Fail "NPC assignment failed"
    }
    
    # 4.6 Watch Educational Videos
    Write-SubSection "4.6 Watch Educational Videos (6)"
    $videoTypes = @("DEPOSIT", "STOCK", "BOND", "PENSION", "FUND", "INSURANCE")
    $videoAllPassed = $true
    foreach ($videoType in $videoTypes) {
        $video = Invoke-Api -Method POST -Endpoint "/v1/tutorial/complete-video" -Body @{
            videoType = $videoType
        }
        if ($video.Success -and $video.Data.data.completed) {
            Write-Detail "  Video: $videoType [OK]"
        } else {
            Write-Detail "  Video: $videoType [FAIL]"
            $videoAllPassed = $false
        }
    }
    if ($videoAllPassed) {
        Write-Success "All educational videos watched"
    } else {
        Write-Fail "Some videos failed"
    }
    
    # 4.7 Pass Preferential Rate Quiz
    Write-SubSection "4.7 Pass Preferential Rate Quiz (6)"
    $productTypes = @("DEPOSIT", "STOCK", "BOND", "PENSION", "FUND", "INSURANCE")
    $quizAllPassed = $true
    foreach ($productType in $productTypes) {
        $quiz = Invoke-Api -Method POST -Endpoint "/v1/tutorial/submit-quiz" -Body @{
            productType = $productType
        }
        if ($quiz.Success -and $quiz.Data.data.correct) {
            Write-Detail "  Quiz: $productType [OK]"
        } else {
            Write-Detail "  Quiz: $productType [FAIL]"
            $quizAllPassed = $false
        }
    }
    if ($quizAllPassed) {
        Write-Success "All quizzes passed"
    } else {
        Write-Fail "Some quizzes failed"
    }
    
    # 4.8 Start Game
    Write-SubSection "4.8 Start Tutorial Game"
    $tutorialStart = Invoke-Api -Method POST -Endpoint "/v1/tutorial/start" -Body @{
        income = @{ monthlyIncome = 2800000 }
        expense = @{ monthlyFixedExpense = 1500000 }
    }
    
    if ($tutorialStart.Success) {
        Write-Success "Tutorial game started"
        Write-Info "Round: $($tutorialStart.Data.data.currentRound.roundNo)"
        Write-Info "Initial Cash: $(Format-Won $tutorialStart.Data.data.portfolio.holdings.cash)"
    } else {
        Write-Fail "Tutorial game start failed"
    }
    
    # 4.9 Progress 6 Rounds
    Write-SubSection "4.9 Tutorial 6 Rounds Progress"
    
    $roundsCompleted = 0
    $lifeEventsResolved = 0
    
    for ($round = 1; $round -le 5; $round++) {
        Write-Host "`n  [Round $round -> $($round + 1)]" -ForegroundColor Yellow
        
        # Various financial activities per round
        $roundRequest = @{}
        
        switch ($round) {
            1 {
                # Round 1: Deposit + Stock buy
                $roundRequest = @{
                    deposits = @(@{ productKey = "DEPOSIT"; amount = 500000 })
                    stockBuys = @(@{ stockId = "STOCK_01"; quantity = 10 })
                }
            }
            2 {
                # Round 2: Fund buy + Insurance
                $roundRequest = @{
                    fundBuys = @(@{ fundId = "FUND_01"; amount = 300000 })
                    insuranceSubscribes = @(@{ insuranceId = "INSURANCE_BASIC" })
                }
            }
            3 {
                # Round 3: Savings
                $roundRequest = @{
                    savings = @(@{ productKey = "SAVING_A"; monthlyAmount = 200000 })
                }
            }
            4 {
                # Round 4: More stock
                $roundRequest = @{
                    stockBuys = @(@{ stockId = "STOCK_02"; quantity = 5 })
                }
            }
            default {
                $roundRequest = @{}
            }
        }
        
        $roundResp = Invoke-Api -Method POST -Endpoint "/v1/tutorial/proceed-round" -Body $roundRequest
        
        if ($roundResp.Success) {
            $roundsCompleted++
            $cash = $roundResp.Data.data.portfolio.holdings.cash
            $netWorth = $roundResp.Data.data.portfolio.summary.netWorth
            Write-Info "Cash: $(Format-Won $cash) | Net Worth: $(Format-Won $netWorth)"
            
            # Check life event
            $lifeEvent = $roundResp.Data.data.roundStart.lifeEvent
            if ($lifeEvent) {
                Write-Host "    [LIFE EVENT!] $($lifeEvent.eventKey)" -ForegroundColor Magenta
                Write-Host "    Amount: $(Format-Won $lifeEvent.amount)" -ForegroundColor Magenta
                
                # Resolve with cash
                $resolveBody = @{
                    eventKey = $lifeEvent.eventKey
                    resolutionType = "CASH"
                    cashAmount = [Math]::Abs($lifeEvent.amount)
                }
                
                $resolve = Invoke-Api -Method POST -Endpoint "/v1/tutorial/resolve-life-event" -Body $resolveBody -SuppressError
                if ($resolve.Success -and $resolve.Data.data.resolved) {
                    Write-Host "    [RESOLVED] Cash payment" -ForegroundColor Green
                    $lifeEventsResolved++
                } else {
                    # If cash insufficient, use loan
                    $loanBody = @{
                        eventKey = $lifeEvent.eventKey
                        resolutionType = "LOAN"
                        loanAmount = [Math]::Abs($lifeEvent.amount)
                    }
                    $loanResolve = Invoke-Api -Method POST -Endpoint "/v1/tutorial/resolve-life-event" -Body $loanBody -SuppressError
                    if ($loanResolve.Success -and $loanResolve.Data.data.resolved) {
                        Write-Host "    [RESOLVED] Loan used" -ForegroundColor Yellow
                        $lifeEventsResolved++
                    } else {
                        Write-Host "    [NOT RESOLVED]" -ForegroundColor Red
                    }
                }
            }
            
            # Use advice at round 3
            if ($round -eq 3) {
                $advice = Invoke-Api -Method POST -Endpoint "/v1/tutorial/use-advice" -Body @{ roundNo = $round + 1 } -SuppressError
                if ($advice.Success) {
                    Write-Host "    [NPC ADVICE USED]" -ForegroundColor Cyan
                    $adviceData = $advice.Data.data
                    Write-Host "    Remaining: $($adviceData.remainingAdviceCount)" -ForegroundColor Cyan
                    # Check hint data
                    if ($adviceData.hint) {
                        Write-Host "    [HINT] Type: $($adviceData.hint.hintType), Target: $($adviceData.hint.target)" -ForegroundColor Magenta
                        Write-Host "           Prediction: $($adviceData.hint.prediction), Next Round: $($adviceData.hint.nextRound)" -ForegroundColor Magenta
                    }
                }
            }
        } else {
            Write-Host "    Round progress failed" -ForegroundColor Red
        }
    }
    
    Write-Host ""
    if ($roundsCompleted -eq 5) {
        Write-Success "Tutorial $($roundsCompleted + 1) rounds completed"
    } else {
        Write-Fail "Tutorial some rounds failed ($roundsCompleted/5 completed)"
    }
    
    if ($lifeEventsResolved -gt 0) {
        Write-Info "Life events resolved: $lifeEventsResolved"
    }
    
    # 4.10 Tutorial Result
    Write-SubSection "4.10 Tutorial Result"
    $tutorialResult = Invoke-Api -Method GET -Endpoint "/v1/tutorial/result"
    
    if ($tutorialResult.Success) {
        $result = $tutorialResult.Data.data
        Write-Success "Tutorial result retrieved"
        Write-Host ""
        Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
        Write-Host "  |          TUTORIAL FINAL RESULT              |" -ForegroundColor Cyan
        Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
        Write-Host "  |  Initial Cash:    $(Format-Won $result.initialCash)" -ForegroundColor White
        Write-Host "  |  Final Net Worth: $(Format-Won $result.finalNetWorth)" -ForegroundColor Green
        Write-Host "  |  Profit:          $(Format-Won $result.profit)" -ForegroundColor Green
        Write-Host "  |  Return Rate:     $($result.returnRate)" -ForegroundColor Green
        Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
        Write-Host "  |  Total Score:     $($result.score.totalScore)" -ForegroundColor Yellow
        Write-Host "  |  Financial Mgmt:  $($result.score.financialManagement)" -ForegroundColor White
        Write-Host "  |  Risk Mgmt:       $($result.score.riskManagement)" -ForegroundColor White
        Write-Host "  |  Return Score:    $($result.score.returnRate)" -ForegroundColor White
        Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
        Write-Host "  |  Insurance:       $($result.insuranceSubscribed)" -ForegroundColor White
        Write-Host "  |  Loan Used:       $($result.loanUsed)" -ForegroundColor White
        Write-Host "  |  Advice Used:     $($result.adviceUsedCount)" -ForegroundColor White
        Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
    } else {
        Write-Fail "Tutorial result retrieval failed"
    }
}

# ===================================================================
# 5. Competition Full Play
# ===================================================================
if (-not $SkipCompetition) {
    Write-Section "Competition (12 Rounds) Full Play"
    
    # 5.1 Reset existing game
    Write-SubSection "5.1 Reset Existing Game"
    $compReset = Invoke-Api -Method DELETE -Endpoint "/v1/competition/reset" -SuppressError
    Write-Info "Reset complete"
    
    # 5.2 Select NPC
    Write-SubSection "5.2 Select NPC"
    $npcSelect = Invoke-Api -Method POST -Endpoint "/v1/competition/select-npc" -Body @{
        npcType = "CHAEWOOMI"
    }
    if ($npcSelect.Success -and $npcSelect.Data.data.selected) {
        Write-Success "NPC selected (CHAEWOOMI)"
    } else {
        Write-Fail "NPC selection failed"
    }
    
    # 5.3 Start Game
    Write-SubSection "5.3 Start Competition Game"
    $compStart = Invoke-Api -Method POST -Endpoint "/v1/competition/start" -Body @{}
    
    if ($compStart.Success) {
        Write-Success "Competition game started"
        Write-Info "Round: $($compStart.Data.data.currentRound.roundNo)"
        Write-Info "Initial Cash: $(Format-Won $compStart.Data.data.portfolio.holdings.cash)"
    } else {
        Write-Fail "Competition game start failed"
    }
    
    # 5.4 Progress 12 Rounds
    Write-SubSection "5.4 Competition 12 Rounds Progress"
    
    $compRoundsCompleted = 0
    $compLifeEventsResolved = 0
    
    for ($round = 1; $round -le 11; $round++) {
        Write-Host "`n  [Round $round -> $($round + 1)]" -ForegroundColor Yellow
        
        $roundRequest = @{}
        
        switch ($round) {
            1 {
                $roundRequest = @{
                    deposits = @(@{ productKey = "DEPOSIT"; amount = 1000000 })
                    stockBuys = @(@{ stockId = "STOCK_01"; quantity = 20 })
                }
            }
            2 {
                $roundRequest = @{
                    fundBuys = @(@{ fundId = "FUND_01"; amount = 500000 })
                    insuranceSubscribes = @(@{ insuranceId = "INSURANCE_BASIC" })
                }
            }
            3 {
                $roundRequest = @{
                    savings = @(@{ productKey = "SAVING_B"; monthlyAmount = 200000 })
                    stockBuys = @(@{ stockId = "STOCK_02"; quantity = 10 })
                }
            }
            4 {
                $roundRequest = @{
                    bonds = @(@{ productKey = "NATIONAL_BOND"; amount = 500000 })
                }
            }
            5 {
                $roundRequest = @{
                    pensions = @(@{ productKey = "PERSONAL_PENSION"; monthlyAmount = 50000 })
                }
            }
            6 {
                $roundRequest = @{
                    fundBuys = @(@{ fundId = "FUND_02"; amount = 300000 })
                }
            }
            7 {
                $roundRequest = @{
                    stockBuys = @(@{ stockId = "STOCK_03"; quantity = 15 })
                }
            }
            8 {
                $roundRequest = @{
                    stockSells = @(@{ stockId = "STOCK_01"; quantity = 5 })
                }
            }
            9 {
                $roundRequest = @{
                    bonds = @(@{ productKey = "CORPORATE_BOND"; amount = 300000 })
                }
            }
            10 {
                $roundRequest = @{
                    fundSells = @(@{ fundId = "FUND_01"; amount = 200000 })
                }
            }
            default {
                $roundRequest = @{}
            }
        }
        
        $roundResp = Invoke-Api -Method POST -Endpoint "/v1/competition/proceed-round" -Body $roundRequest
        
        if ($roundResp.Success) {
            $compRoundsCompleted++
            $cash = $roundResp.Data.data.portfolio.holdings.cash
            $netWorth = $roundResp.Data.data.portfolio.summary.netWorth
            Write-Info "Cash: $(Format-Won $cash) | Net Worth: $(Format-Won $netWorth)"
            
            # Check life event
            $lifeEvent = $roundResp.Data.data.roundStart.lifeEvent
            if ($lifeEvent) {
                Write-Host "    [LIFE EVENT!] $($lifeEvent.eventKey)" -ForegroundColor Magenta
                Write-Host "    Amount: $(Format-Won $lifeEvent.amount)" -ForegroundColor Magenta
                
                # Try insurance first if insurable
                if ($lifeEvent.insurableEvent) {
                    $resolveBody = @{
                        eventKey = $lifeEvent.eventKey
                        resolutionType = "INSURANCE"
                    }
                    $resolve = Invoke-Api -Method POST -Endpoint "/v1/competition/resolve-life-event" -Body $resolveBody -SuppressError
                    
                    if ($resolve.Success -and $resolve.Data.data.resolved) {
                        Write-Host "    [RESOLVED] Insurance applied" -ForegroundColor Green
                        $compLifeEventsResolved++
                    } else {
                        # Fall back to cash
                        $cashBody = @{
                            eventKey = $lifeEvent.eventKey
                            resolutionType = "CASH"
                            cashAmount = [Math]::Abs($lifeEvent.amount)
                        }
                        $cashResolve = Invoke-Api -Method POST -Endpoint "/v1/competition/resolve-life-event" -Body $cashBody -SuppressError
                        if ($cashResolve.Success -and $cashResolve.Data.data.resolved) {
                            Write-Host "    [RESOLVED] Cash payment" -ForegroundColor Green
                            $compLifeEventsResolved++
                        }
                    }
                } else {
                    # Cash payment
                    $cashBody = @{
                        eventKey = $lifeEvent.eventKey
                        resolutionType = "CASH"
                        cashAmount = [Math]::Abs($lifeEvent.amount)
                    }
                    $resolve = Invoke-Api -Method POST -Endpoint "/v1/competition/resolve-life-event" -Body $cashBody -SuppressError
                    if ($resolve.Success -and $resolve.Data.data.resolved) {
                        Write-Host "    [RESOLVED] Cash payment" -ForegroundColor Green
                        $compLifeEventsResolved++
                    }
                }
            }
            
            # Use advice at rounds 4, 8
            if ($round -eq 4 -or $round -eq 8) {
                $advice = Invoke-Api -Method POST -Endpoint "/v1/competition/use-advice" -Body @{ roundNo = $round + 1 } -SuppressError
                if ($advice.Success) {
                    Write-Host "    [NPC ADVICE USED]" -ForegroundColor Cyan
                    $adviceData = $advice.Data.data
                    Write-Host "    Remaining: $($adviceData.remainingAdviceCount)" -ForegroundColor Cyan
                    # Check hint data
                    if ($adviceData.hint) {
                        Write-Host "    [HINT] Type: $($adviceData.hint.hintType), Target: $($adviceData.hint.target)" -ForegroundColor Magenta
                        Write-Host "           Prediction: $($adviceData.hint.prediction), Next Round: $($adviceData.hint.nextRound)" -ForegroundColor Magenta
                    }
                }
            }
        } else {
            Write-Host "    Round progress failed" -ForegroundColor Red
        }
    }
    
    Write-Host ""
    if ($compRoundsCompleted -eq 11) {
        Write-Success "Competition 12 rounds completed"
    } else {
        Write-Fail "Competition some rounds failed ($compRoundsCompleted/11 completed)"
    }
    
    if ($compLifeEventsResolved -gt 0) {
        Write-Info "Life events resolved: $compLifeEventsResolved"
    }
    
    # 5.5 Competition Result
    Write-SubSection "5.5 Competition Result"
    $compResult = Invoke-Api -Method GET -Endpoint "/v1/competition/result"
    
    if ($compResult.Success) {
        $result = $compResult.Data.data
        Write-Success "Competition result retrieved"
        Write-Host ""
        Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
        Write-Host "  |        COMPETITION FINAL RESULT             |" -ForegroundColor Cyan
        Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
        Write-Host "  |  Initial Cash:    $(Format-Won $result.initialCash)" -ForegroundColor White
        Write-Host "  |  Final Net Worth: $(Format-Won $result.finalNetWorth)" -ForegroundColor Green
        Write-Host "  |  Profit:          $(Format-Won $result.profit)" -ForegroundColor Green
        Write-Host "  |  Return Rate:     $($result.returnRate)" -ForegroundColor Green
        Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
        Write-Host "  |  Total Score:     $($result.score.totalScore)" -ForegroundColor Yellow
        Write-Host "  |  Financial Mgmt:  $($result.score.financialManagement)" -ForegroundColor White
        Write-Host "  |  Risk Mgmt:       $($result.score.riskManagement)" -ForegroundColor White
        Write-Host "  |  Return Score:    $($result.score.returnRate)" -ForegroundColor White
        Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
        Write-Host "  |  Insurance:       $($result.insuranceSubscribed)" -ForegroundColor White
        Write-Host "  |  Loan Used:       $($result.loanUsed)" -ForegroundColor White
        Write-Host "  |  Illegal Loan:    $($result.illegalLoanUsed)" -ForegroundColor White
        Write-Host "  |  Advice Used:     $($result.adviceUsedCount)" -ForegroundColor White
        Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
    } else {
        Write-Fail "Competition result retrieval failed"
    }
}

# ===================================================================
# 6. Achievements Check
# ===================================================================
Write-Section "Achievements System Check"

Write-SubSection "6.1 Achievements List"
$achievements = Invoke-Api -Method GET -Endpoint "/v1/achievements"

if ($achievements.Success) {
    $ach = $achievements.Data.data
    Write-Success "Achievements list retrieved"
    Write-Host ""
    Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
    Write-Host "  |           ACHIEVEMENTS STATUS               |" -ForegroundColor Cyan
    Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
    Write-Host "  |  Total:    $($ach.totalAchievements)" -ForegroundColor White
    Write-Host "  |  Achieved: $($ach.achievedCount)" -ForegroundColor Green
    Write-Host "  |  Rate:     $($ach.achievementRate)" -ForegroundColor Yellow
    Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
    
    Write-Host "`n  Achieved:" -ForegroundColor Yellow
    foreach ($achievement in $ach.achievements | Where-Object { $_.achieved }) {
        Write-Host "    [OK] [$($achievement.achievementId)] $($achievement.name)" -ForegroundColor Green
        Write-Host "         $($achievement.description)" -ForegroundColor Gray
    }
    
    Write-Host "`n  In Progress:" -ForegroundColor Yellow
    foreach ($achievement in $ach.achievements | Where-Object { -not $_.achieved -and $_.progress -gt 0 }) {
        Write-Host "    [..] [$($achievement.achievementId)] $($achievement.name) ($($achievement.progress)%)" -ForegroundColor Yellow
        Write-Host "         $($achievement.description)" -ForegroundColor Gray
    }
} else {
    Write-Fail "Achievements list retrieval failed"
}

# ===================================================================
# 7. Rankings Check
# ===================================================================
Write-Section "Ranking System Check"

Write-SubSection "7.1 Ranking List"
$ranking = Invoke-Api -Method GET -Endpoint "/v1/competition/ranking"

if ($ranking.Success) {
    $rank = $ranking.Data.data
    Write-Success "Ranking retrieved"
    Write-Host ""
    Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
    Write-Host "  |             RANKING STATUS                  |" -ForegroundColor Cyan
    Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
    Write-Host "  |  My Rank:       #$($rank.myRank)" -ForegroundColor Yellow
    Write-Host "  |  Total Players: $($rank.totalPlayers)" -ForegroundColor White
    Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
    
    Write-Host "`n  Top Rankings:" -ForegroundColor Yellow
    foreach ($player in $rank.rankings | Select-Object -First 5) {
        $meIndicator = if ($player.isMe) { " <- ME" } else { "" }
        $color = if ($player.isMe) { "Green" } else { "White" }
        Write-Host "    #$($player.rank). $($player.nickname) - $($player.totalScore) pts ($(Format-Won $player.finalNetWorth))$meIndicator" -ForegroundColor $color
    }
} else {
    Write-Fail "Ranking retrieval failed"
}

# ===================================================================
# 7.2 Monthly Ranking
# ===================================================================
Write-SubSection "7.2 Monthly Ranking"

# Current month ranking (yearMonth는 자동으로 현재 월 사용)
$monthlyRanking = Invoke-Api -Method GET -Endpoint "/v1/competition/monthly-ranking?limit=10"

if ($monthlyRanking.Success) {
    $monthRank = $monthlyRanking.Data.data
    Write-Success "Monthly Ranking retrieved"
    Write-Host ""
    Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
    Write-Host "  |          MONTHLY RANKING ($($monthRank.yearMonth))            |" -ForegroundColor Cyan
    Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
    Write-Host "  |  Year-Month:  $($monthRank.yearMonth)" -ForegroundColor White
    Write-Host "  |  Total Count: $($monthRank.totalCount)" -ForegroundColor White
    Write-Host "  |  Source:      $($monthRank.source)" -ForegroundColor White
    Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
    
    Write-Host "`n  Top Monthly Rankings:" -ForegroundColor Yellow
    foreach ($player in $monthRank.rankings | Select-Object -First 5) {
        Write-Host "    #$($player.rank). $($player.nickname) (NPC:$($player.npcNo)) - $($player.totalScore) pts" -ForegroundColor White
    }
    
    # My Monthly Rank
    if ($monthRank.myRank) {
        Write-Host "`n  My Monthly Rank:" -ForegroundColor Yellow
        Write-Host "    Rank: #$($monthRank.myRank.rank)" -ForegroundColor Cyan
        Write-Host "    Score: $($monthRank.myRank.totalScore)" -ForegroundColor Cyan
    }
    
    # Refresh Schedule Info
    if ($monthRank.refreshSchedule) {
        Write-Host "`n  Refresh Schedule:" -ForegroundColor Yellow
        Write-Host "    Enabled: $($monthRank.refreshSchedule.enabled)" -ForegroundColor Gray
        Write-Host "    Day: $($monthRank.refreshSchedule.day)" -ForegroundColor Gray
        Write-Host "    Time: $($monthRank.refreshSchedule.time)" -ForegroundColor Gray
        Write-Host "    Cache TTL: $($monthRank.refreshSchedule.cacheTtl) sec" -ForegroundColor Gray
    }
} else {
    Write-Fail "Monthly Ranking retrieval failed"
}

# ===================================================================
# 7.3 My Info (Competition)
# ===================================================================
Write-SubSection "7.3 My Info (Competition)"
$myInfo = Invoke-Api -Method GET -Endpoint "/v1/competition/my-info"

if ($myInfo.Success) {
    $info = $myInfo.Data.data.myInfo
    Write-Success "My Info retrieved"
    Write-Host ""
    Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
    Write-Host "  |               MY INFO                       |" -ForegroundColor Cyan
    Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
    Write-Host "  |  UID:          $($info.uid)" -ForegroundColor White
    Write-Host "  |  Nickname:     $($info.nickname)" -ForegroundColor White
    Write-Host "  |  NPC No:       $($info.npcNo)" -ForegroundColor White
    Write-Host "  |  NPC Name:     $($info.npcName)" -ForegroundColor White
    Write-Host "  |  Rank:         #$($info.rank)" -ForegroundColor Yellow
    Write-Host "  |  Best Score:   $($info.bestScore)" -ForegroundColor Green
    Write-Host "  |  Games Played: $($info.totalGamesPlayed)" -ForegroundColor White
    Write-Host "  |  Tutorial:     $($info.tutorialCompleted)" -ForegroundColor White
    Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
} else {
    Write-Fail "My Info retrieval failed"
}

# ===================================================================
# 7.4 Portfolio (Other User)
# ===================================================================
Write-SubSection "7.4 Portfolio (Other User)"

# Test with a sample uid "user001"
$targetUid = "user001"
$portfolio = Invoke-Api -Method GET -Endpoint "/v1/competition/portfolio/$targetUid"

if ($portfolio.Success) {
    $portData = $portfolio.Data.data
    Write-Success "Portfolio retrieved for $targetUid"
    Write-Host ""
    Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
    Write-Host "  |           USER PORTFOLIO ($targetUid)         |" -ForegroundColor Cyan
    Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
    if ($portData.userInfo) {
        Write-Host "  |  Nickname:  $($portData.userInfo.nickname)" -ForegroundColor White
        Write-Host "  |  NPC No:    $($portData.userInfo.npcNo)" -ForegroundColor White
        Write-Host "  |  Rank:      #$($portData.userInfo.rank)" -ForegroundColor Yellow
        Write-Host "  |  Score:     $($portData.userInfo.bestScore)" -ForegroundColor Green
    }
    if ($portData.portfolio) {
        Write-Host "  |  Cash:      $(Format-Won $portData.portfolio.cash)" -ForegroundColor White
        Write-Host "  |  Net Worth: $(Format-Won $portData.portfolio.netWorth)" -ForegroundColor Green
    }
    Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
    
    # Portfolio Composition
    if ($portData.composition) {
        Write-Host "`n  Portfolio Composition:" -ForegroundColor Yellow
        foreach ($item in $portData.composition) {
            Write-Host "    $($item.assetType): $($item.percentage)% ($(Format-Won $item.amount))" -ForegroundColor White
        }
    }
} else {
    Write-Info "Portfolio for $targetUid not found (may not exist)"
}

# ===================================================================
# 8. Logout
# ===================================================================
Write-Section "User Logout"

Write-SubSection "8.1 Logout (POST /v1/user/logout)"
$logout = Invoke-Api -Method POST -Endpoint "/v1/user/logout" -Body @{}
if ($logout.Success -and $logout.Data.data.success) {
    Write-Success "Logout successful"
    Write-Info "Logout time: $($logout.Data.data.logoutTime)"
} else {
    Write-Info "Logout skipped or failed"
}

# ===================================================================
# 9. Final Summary
# ===================================================================
Write-Section "Test Results Summary"

Write-Host ""
Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
Write-Host "  |             TEST RESULTS                    |" -ForegroundColor Cyan
Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan
Write-Host "  |  Total Tests: $($script:totalTests)" -ForegroundColor White
Write-Host "  |  Passed:      $($script:passedTests)" -ForegroundColor Green
$failColor = if ($script:failedTests -gt 0) { "Red" } else { "White" }
Write-Host "  |  Failed:      $($script:failedTests)" -ForegroundColor $failColor
$rateColor = if ($script:failedTests -eq 0) { "Green" } else { "Yellow" }
$rate = if ($script:totalTests -gt 0) { [math]::Round($script:passedTests / $script:totalTests * 100, 1) } else { 0 }
Write-Host "  |  Success Rate: $rate%" -ForegroundColor $rateColor
Write-Host "  +---------------------------------------------+" -ForegroundColor Cyan

Write-Host "`n  UID: $Uid" -ForegroundColor Gray
Write-Host "  Completed: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Gray

if ($script:failedTests -eq 0) {
    Write-Host "`n  * ALL TESTS PASSED! *" -ForegroundColor Green
} else {
    Write-Host "`n  ! Some tests failed. Check logs above." -ForegroundColor Yellow
}

Write-Host "`n" + ("=" * 60) -ForegroundColor Cyan
Write-Host " Full Test Complete!" -ForegroundColor Cyan
Write-Host ("=" * 60) + "`n" -ForegroundColor Cyan

