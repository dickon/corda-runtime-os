@Library('corda-shared-build-pipeline-steps@owen/CORE-17383-Add-token-selection-worker') _

// This build forces using the "very latest" version of the dependencies, regardless of which revision was chosen
//  This is useful as it gives early indication of a downstream change that may introduce a breaking change
//  It should not, however, be a PR gate.
cordaCompatibilityCheckPipeline(
    useLatestBeta: true,
)
