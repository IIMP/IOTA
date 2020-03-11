#!/bin/bash
current_dir=$(pwd)
cd /root/IOTA/compass/docs/private_tangle/
scriptdir=$(pwd)
. lib.sh
load_config
COO_ADDRESS=EQFUFPJODWV9OBIQAWFABRFHYQZQVZJKVKTDALDWGWVSGIZAYHLUKXRXKAGYTMKHMMRGVXU9TOUGUOQE9
cd $current_dir
java -jar /root/IOTA/iri/target/iri-1.8.4.jar \
       --testnet true \
       --remote true \
       --testnet-coordinator $COO_ADDRESS \
       --testnet-coordinator-security-level $security \
       --testnet-coordinator-signature-mode $sigMode \
       --mwm $mwm \
       --milestone-start $milestoneStart \
       --milestone-keys $depth \
       --snapshot /root/IOTA/compass/docs/private_tangle/snapshot.txt \
       --max-depth 1000 \
       --port ${current_dir##*/} \
       --neighbors "tcp://127.0.0.1:15600" \
       -t 15601
