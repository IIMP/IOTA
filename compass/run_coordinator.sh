#!/bin/bash
rep=({0..3})
if [[ $# -gt 0 ]]; then
    rep=($@)
fi
for i in "${rep[@]}"; do
    echo "starting coordinator $i"
    #valgrind --leak-check=full ./examples/hotstuff-app --conf hotstuff-sec${i}.conf > log${i} 2>&1 &
    #gdb -ex r -ex bt -ex q --args ./examples/hotstuff-app --conf hotstuff-sec${i}.conf > log${i} 2>&1 &
    ./docs/private_tangle/03_run_coordinator.sh -inception -Mulitiple -broadcast -hotstuff_port 1006${i} -hotstuff_recv_port 1008${i} -host "http://localhost:1426$i" > log${i} 2>&1 &
done
wait


