/*
*******************************************************************************    
*   BTChip Bitcoin Hardware Wallet Java API
*   (c) 2014 BTChip - 1BTChip7VfTnrPra5jqci7ejnMguuHogTn
*   
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*   limitations under the License.
********************************************************************************
*/

package com.btchip;

import com.btchip.utils.Dump;

public interface BTChipConstants {

    byte BTCHIP_CLA = (byte)0xE0;
    byte BTCHIP_ADM_CLA = (byte)0xD0;

    byte BTCHIP_INS_SETUP = (byte)0x20;
    byte BTCHIP_INS_VERIFY_PIN = (byte)0x22;
    byte BTCHIP_INS_GET_OPERATION_MODE = (byte)0x24;
    byte BTCHIP_INS_SET_OPERATION_MODE = (byte)0x26;
    byte BTCHIP_INS_SET_KEYMAP = (byte)0x28;
    byte BTCHIP_INS_SET_COMM_PROTOCOL = (byte)0x2a;
    byte BTCHIP_INS_GET_WALLET_PUBLIC_KEY = (byte)0x40;
    byte BTCHIP_INS_GET_LIQUID_BLINDING_KEY = (byte)0xE2;
    byte BTCHIP_INS_GET_LIQUID_NONCE = (byte) 0xE4;
    byte BTCHIP_INS_GET_LIQUID_COMMITMENTS = (byte) 0xE0;
    byte BTCHIP_INS_GET_LIQUID_BLINDING_FACTOR = (byte) 0xE8;
    byte BTCHIP_INS_GET_LIQUID_ISSUANCE_INFORMATION = (byte) 0xE6;
    byte BTCHIP_INS_GET_LIQUID_GREEN_ADDRESS = (byte) 0xEA;
    byte BTCHIP_INS_GET_TRUSTED_INPUT = (byte)0x42;
    byte BTCHIP_INS_HASH_INPUT_START = (byte)0x44;
    byte BTCHIP_INS_HASH_INPUT_FINALIZE = (byte)0x46;
    byte BTCHIP_INS_HASH_SIGN = (byte)0x48;
    byte BTCHIP_INS_HASH_INPUT_FINALIZE_FULL = (byte)0x4a;
    byte BTCHIP_INS_GET_INTERNAL_CHAIN_INDEX = (byte)0x4c;
    byte BTCHIP_INS_SIGN_MESSAGE = (byte)0x4e;
    byte BTCHIP_INS_GET_TRANSACTION_LIMIT = (byte)0xa0;
    byte BTCHIP_INS_SET_TRANSACTION_LIMIT = (byte)0xa2;
    byte BTCHIP_INS_IMPORT_PRIVATE_KEY = (byte)0xb0;
    byte BTCHIP_INS_GET_PUBLIC_KEY = (byte)0xb2;
    byte BTCHIP_INS_DERIVE_BIP32_KEY = (byte)0xb4;
    byte BTCHIP_INS_SIGNVERIFY_IMMEDIATE = (byte)0xb6;
    byte BTCHIP_INS_GET_RANDOM = (byte)0xc0;
    byte BTCHIP_INS_GET_ATTESTATION = (byte)0xc2;
    byte BTCHIP_INS_GET_FIRMWARE_VERSION = (byte)0xc4;
    byte BTCHIP_INS_COMPOSE_MOFN_ADDRESS = (byte)0xc6;
    byte BTCHIP_INS_GET_POS_SEED = (byte)0xca;

    // Nano X stuff
    byte BTCHIP_CLA_COMMON_SDK = (byte) 0xB0;
    byte BTCHIP_INS_GET_APP_NAME_AND_VERSION = (byte) 0x01;

    byte BTCHIP_INS_ADM_SET_KEYCARD_SEED = (byte)0x26;
    
    byte[] QWERTY_KEYMAP = Dump.hexToBin("000000000000000000000000760f00d4ffffffc7000000782c1e3420212224342627252e362d3738271e1f202122232425263333362e37381f0405060708090a0b0c0d0e0f101112131415161718191a1b1c1d2f3130232d350405060708090a0b0c0d0e0f101112131415161718191a1b1c1d2f313035");
    byte[] AZERTY_KEYMAP = Dump.hexToBin("08000000010000200100007820c8ffc3feffff07000000002c38202030341e21222d352e102e3637271e1f202122232425263736362e37101f1405060708090a0b0c0d0e0f331112130415161718191d1b1c1a2f64302f2d351405060708090a0b0c0d0e0f331112130415161718191d1b1c1a2f643035");

    // Architectures
    byte BTCHIP_ARCH_LEDGER_1 = (byte) 0x20;
    byte BTCHIP_ARCH_NANO_SX = (byte) 0x30;

    // SW reply codes
    int SW_OK = 0x9000;
    int SW_USER_REJECT = 0x6985;
    int SW_INS_NOT_SUPPORTED = 0x6D00;
    int SW_WRONG_P1_P2 = 0x6B00;
    int SW_INCORRECT_P1_P2 = 0x6A86;
}
