package io.bisq.network.p2p;


import io.bisq.common.persistance.Msg;

public interface SendersNodeAddressMsg extends Msg {
    NodeAddress getSenderNodeAddress();
}