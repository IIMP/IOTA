#!/bin/bash

scriptdir=$(dirname "$(readlink -f "$0")")
. $scriptdir/lib.sh

load_config

bazel run //compass:coordinator \
	-layers $scriptdir/data/layers \
	-statePath $scriptdir/data/compass.state \
	-sigMode $sigMode \
	-powMode $powMode \
	-mwm $mwm \
	-security $security \
	-seed $seed \
	-tick $tick \
	-host $host \
	"$@"
