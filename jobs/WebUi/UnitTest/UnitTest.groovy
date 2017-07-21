def UnitTest = load("jobs/UnitTest/UnitTest.groovy")

UnitTest.test_repos = ["on-wss", "on-web-ui"]
UnitTest.setRunScript("jobs/WebUi/UnitTest/unit_test.sh")

return UnitTest