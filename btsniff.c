/* -*- compile-command: "gcc -ggdb -lnids -lssl -lefence -o btsniff btsniff.c" -*- */
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/in_systm.h>
#include <arpa/inet.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <nids.h>
#include <openssl/bn.h>

#define int_ntoa(x) inet_ntoa(*((struct in_addr *)&x))

#define BT_HDR "\x13""BitTorrent protocol"
#define BT_HDR_LEN 20
#define BT_RES_LEN 8
#define BT_INFOHASH_LEN 20
#define BT_ID_LEN 20
#define BT_SENDRSA_HDR_LEN (4 + 1 + 4 + 4)

#define BT_SENDRSA_ID 10
#define BT_SENDSYM_ID 11

void print_str(const char* data, int len)
{
    fwrite(data, 1, len, stderr);
}

void print_hex(const char* data, int len)
{
    int i;
    for (i = 0; i < len; i++) {
        fprintf(stderr, "%02X", (unsigned char) data[i]);
    }
    fprintf(stderr, "\n");
}

int concat(char* dest, int destlen, char* src, int srclen)
{
    int i;
    for (i = 0; i < srclen; i++) {
        dest[destlen + i] = src[i];
    }
    return destlen + srclen;
}

int comp(const char* s1, const char* s2, size_t len)
{
    int i;
    for (i = 0; i < (len - 1) && *s1 == *s2; ++i, ++s1, ++s2);
    return *(const unsigned char*)s1 - *(const unsigned char *) s2;
}

/* Computes a^p (mod m) where a is between 0 and 255 and
   p and m are ASCII strings */
BIGNUM* modexp(char a, BIGNUM* p, BIGNUM* m, BN_CTX* ctx)
{
    BIGNUM* _a = BN_bin2bn(&a, 1, 0);
    BIGNUM* ret = BN_new();
    BN_mod_exp(ret, _a, p, m, ctx);
    return ret;
}
/* Put in clear the byte corresponding to the encrypted text in cypher
   using the correspondance table enc_table[].
   Return 0 if the byte was successfully decrypted, 1 else */
int uncypher(BIGNUM* cypher, BIGNUM* enc_table[], unsigned char* clear)
{
    int i;
    for (i = 0; i < 256; i++) {
        if (BN_cmp(cypher, enc_table[i]) == 0) break;
    }
    if (i == 256) {
        return 1;
    }
    *clear = i;
    return 0;
}

/* Returns a string which contains the cleartext corresponding to cypher
   of length cypher_len. Each byte is encoded with length key_len, this means
   that cypher_len MUST be a multiple of key_len.
   Returns 0 if a char could not be uncyphered.
   The returned array has length cypher_len/key_len.
   enc_table[] provides the correspondance between cypher and clear byte.
   The calling function is responsible for free()-ing the returned array. */
char* uncypher_msg(unsigned char* cypher, int cypher_len, int key_len, BIGNUM* enc_table[])
{
    assert(cypher_len % key_len == 0);
    int clear_len = cypher_len/key_len;
    unsigned char *ret = malloc(clear_len);
    int i;
    for (i = 0; i < clear_len; i++) {
        //fprintf(stderr, "uncyphering byte %d:\n", i);
        BIGNUM *byte = BN_bin2bn(cypher + key_len*i, key_len, 0);
        int ok = uncypher(byte, enc_table, &ret[i]);
        if (ok != 0) {
            return 0;
        }
    }
    return ret;
}

/* Generates encryption table:
   a[i] = i^p (mod m) with 0 <= i <= 255 */
BIGNUM** gen_table(BIGNUM *p, BIGNUM *m)
{
    BIGNUM** ret = malloc(256*sizeof(ret));
    BN_CTX* ctx = BN_CTX_new();
    int i;
    for (i = 0; i < 256; i++) {
        ret[i] = modexp(i, p, m, ctx);
    }
    BN_CTX_free(ctx);
    return ret;
}

/* Return an array containing the exclusive or
   of the arrays a and b of length len */
char* xor(char a[], char b[], int len)
{
    char* ret = malloc(len);
    int i;
    for (i = 0; i < len; i++) {
        ret[i] = a[i] ^ b[i];
    }
    return ret;
}

/* struct tuple4 contains addresses and port numbers of the TCP connections.
   The following auxiliary function produces a string looking like
   10.0.0.1,1024,10.0.0.2,23 */
char* tuple_to_str(struct tuple4 addr)
{
    static char buf[256];
    strcpy(buf, int_ntoa(addr.saddr));
    sprintf(buf + strlen(buf), ",%i,", addr.source);
    strcat(buf, int_ntoa(addr.daddr));
    sprintf(buf + strlen(buf), ",%i", addr.dest);
    return buf;
}

void tcp_stop(struct tcp_stream *a_tcp)
{
    a_tcp->client.collect = 0;
    a_tcp->server.collect = 0;
}

unsigned int to_int(unsigned char array[4])
{
    return array[0] << 24
         | array[1] << 16
         | array[2] << 8
         | array[3];
}

int is_encrypted(const unsigned char* reserved)
{
    int i;
    for (i = 0; i < BT_RES_LEN; i++) {
        fprintf(stderr, "%d\n", (int)reserved[i]);
    }
    return (reserved[7] >> 4) & 1;
}

typedef struct {
    BIGNUM* key;
    unsigned int key_len;
    BIGNUM* mod;
    unsigned int mod_len;
} rsa_t;

typedef struct {
    unsigned char* key;
    unsigned int len;
} xor_t;

typedef struct {
    unsigned char *buf;
    unsigned int buf_len;
    unsigned int enc_byte_len;
    rsa_t rsa;
    xor_t xor;
    BIGNUM **enc_table;
} bt_stream;

void tcp_callback(struct tcp_stream *a_tcp, void **param)
{
    char address_tuple[128];
    strcpy(address_tuple, tuple_to_str(a_tcp->addr));
    switch(a_tcp->nids_state) {
    case NIDS_JUST_EST: {
        a_tcp->client.collect = 1;
        a_tcp->server.collect = 1;
        fprintf(stderr, "# %s established\n", address_tuple);
        return;
    }
    case NIDS_CLOSE: {
        fprintf(stderr, "# %s closing\n", address_tuple);
        return;
    }
    case NIDS_RESET: {
        fprintf(stderr, "# %s reset\n", address_tuple);
        return;
    }
    case NIDS_DATA: {
        struct half_stream hlf;
        if (a_tcp->client.count_new) {
            hlf = a_tcp->client;
            fprintf(stderr, "# %s got data (<-)\n", address_tuple);
        } else {
            hlf = a_tcp->server;
            fprintf(stderr, "# %s got data (->)\n", address_tuple);
        }
        int side = (a_tcp->client.count_new != 0);
        bt_stream** bt_array = (bt_stream**) a_tcp->user;
        if (!bt_array) {
            bt_array = malloc(2*sizeof(*bt_array));
            a_tcp->user = (void*) bt_array;
            int i;
            for (i = 0; i < 2; i++) {
                bt_array[i] = malloc(sizeof(**bt_array));
                bt_array[i]->enc_byte_len = 0;
                bt_array[i]->xor.key = 0;
                bt_array[i]->xor.len = 0;
                bt_array[i]->buf = 0;
                bt_array[i]->buf_len = 0;
                bt_array[i]->enc_table = 0;
            }
        }
        bt_stream* bt = bt_array[side];
        bt_stream* peer_bt = bt_array[1-side];
        int oldlen = bt->buf_len;
        fprintf(stderr, "Got %d bytes\n", hlf.count_new);
        bt->buf = realloc(bt->buf, bt->buf_len + hlf.count_new);
        bt->buf_len = concat(bt->buf, bt->buf_len, hlf.data, hlf.count_new);
        char *bufptr = bt->buf;
        int read = 0;
        if (oldlen < read + BT_HDR_LEN && bt->buf_len >= read + BT_HDR_LEN) {
            if (comp(bufptr, BT_HDR, BT_HDR_LEN) != 0) {
                fprintf(stderr, "Not Bittorent\n");
                tcp_stop(a_tcp);
                return;
            }
            fprintf(stderr, "New BitTorrent stream: %s\n", address_tuple);
        }
        read += BT_HDR_LEN;
        bufptr += BT_HDR_LEN;
        if (oldlen < read + BT_RES_LEN && bt->buf_len >= read + BT_RES_LEN) {
            if (!is_encrypted(bufptr)) {
                fprintf(stderr, "Not encrypted, nothing fun to do.\n");
                tcp_stop(a_tcp);
                return;
            }
            fprintf(stderr, "Peer is encrypted.\n");
        }
        read += BT_RES_LEN;
        bufptr += BT_RES_LEN;
        if (oldlen < read + BT_INFOHASH_LEN && bt->buf_len >= read + BT_INFOHASH_LEN) {
            fprintf(stderr, "info hash:\n");
            print_str(bufptr, BT_INFOHASH_LEN);
            fprintf(stderr, "\n");
        }
        read += BT_INFOHASH_LEN;
        bufptr += BT_INFOHASH_LEN;
        if (oldlen < read + BT_ID_LEN && bt->buf_len >= read + BT_ID_LEN) {
            fprintf(stderr, "peer id:\n");
            print_str(bufptr, BT_ID_LEN);
            fprintf(stderr, "\n");
        }
        read += BT_ID_LEN;
        bufptr += BT_ID_LEN;
        int sendrsa_len = 0;
        if (oldlen < read + BT_SENDRSA_HDR_LEN && bt->buf_len >= read + BT_SENDRSA_HDR_LEN) {
            sendrsa_len = to_int(bufptr);
            if (bufptr[4] != BT_SENDRSA_ID) {
                fprintf(stderr, "Error: Didn't get sendrsa message\n");
                tcp_stop(a_tcp);
                return;
            }
            /* Size in bits for some reason, "+ 1" for sign bit.*/
            bt->enc_byte_len = to_int(bufptr + 4 + 1)/8 + 1;
            bt->rsa.key_len = to_int(bufptr + 4 + 1 + 4);
            fprintf(stderr, "encoded byte len: %d, rsa len: %d\n", bt->enc_byte_len, bt->rsa.key_len);
        }
        read += BT_SENDRSA_HDR_LEN;
        bufptr += BT_SENDRSA_HDR_LEN;
        if (oldlen < read + bt->rsa.key_len && bt->buf_len >= read + bt->rsa.key_len) {
      	    bt->rsa.key = BN_bin2bn(bufptr, bt->rsa.key_len, 0);
            fprintf(stderr, "Copied RSA!\n");
            BN_print_fp(stderr, bt->rsa.key);
            fprintf(stderr, "\n");
        }
        read += bt->rsa.key_len;
        bufptr += bt->rsa.key_len;
        if (oldlen < read + 4 && bt->buf_len >= read + 4) {
            bt->rsa.mod_len = to_int(bufptr);
            fprintf(stderr, "mod size: %d\n", bt->rsa.mod_len);
            assert(sendrsa_len == 1 + 4 + 4 + bt->rsa.key_len + 4 + bt->rsa.mod_len);
        }
        read += 4;
        bufptr += 4;
        if (oldlen < read + bt->rsa.mod_len && bt->buf_len >= read + bt->rsa.mod_len) {
            bt->rsa.mod = BN_bin2bn(bufptr, bt->rsa.mod_len, 0);
            fprintf(stderr, "Copied mod!\n");
            BN_print_fp(stderr, bt->rsa.mod);
            fprintf(stderr, "\n");
            bt->enc_table = gen_table(bt->rsa.key, bt->rsa.mod);
            fprintf(stderr, "Generated enc table!\n");
        }
        read += bt->rsa.mod_len;
        bufptr += bt->rsa.mod_len;

        if (peer_bt->enc_table == 0) {
            return;
        }

        int send_sym_len_hdr = (4 + 1)*peer_bt->enc_byte_len;
        if (oldlen < read + send_sym_len_hdr && bt->buf_len >= read + send_sym_len_hdr) {
            fprintf(stderr, "H4x0ring...\n");
            char *dec_bufptr = uncypher_msg(bufptr, send_sym_len_hdr, peer_bt->enc_byte_len, peer_bt->enc_table);
            if (dec_bufptr == 0) {
                fprintf(stderr, "Decryption failed!\n");
                tcp_stop(a_tcp);
                return;
            }
            bt->xor.len = to_int(dec_bufptr) - 1;
            assert(dec_bufptr[4] == BT_SENDSYM_ID);
            fprintf(stderr, "XOR Key len: %d\n", bt->xor.len);
        }
        read += send_sym_len_hdr;
        bufptr += send_sym_len_hdr;

        int xor_enc_len = bt->xor.len * peer_bt->enc_byte_len;
        if (oldlen < read + xor_enc_len && bt->buf_len >= read + xor_enc_len) {
            bt->xor.key = uncypher_msg(bufptr, xor_enc_len, peer_bt->enc_byte_len, peer_bt->enc_table);
            if (bt->xor.key == 0) {
                fprintf(stderr, "Decryption failed!\n");
                tcp_stop(a_tcp);
                return;
            }
            fprintf(stderr, "XOR Key:\n");
            print_hex(bt->xor.key, bt->xor.len);
            fprintf(stderr, "\n");
        }
        read += xor_enc_len;
        bufptr += xor_enc_len;
        if (peer_bt->xor.key == 0) {
            return;
        }
        int msg_len = -1;
        if (bt->buf_len >= read + 4) {
            msg_len = to_int(xor(bufptr, peer_bt->xor.key, 4));
        }
        read += 4;
        bufptr += 4;

        if (msg_len == -1) {
            return;
        }
        if (oldlen < read + msg_len && bt->buf_len >= read + msg_len) {
            char *dec_bufptr = xor(bufptr, peer_bt->xor.key + 4, msg_len);
            fprintf(stderr, "First XORed message with type %d is:\n", dec_bufptr[0]);
            print_hex(dec_bufptr + 1, msg_len - 1);
        }
    }
    }
}

void syslog(int type, int errnum, struct ip *iph, void *data)
{
    fprintf(stderr, "libnids error: %d - %d\n", type, errnum);
}


int main(int argc, char **argv)
{
    if (argc != 2) {
        fprintf(stderr, "Usage: %s <network interface>\n", argv[0]);
        return 1;
    }
    nids_params.device = argv[1];
    nids_params.syslog = syslog;
    nids_params.scan_num_hosts = 0;
    if (!nids_init()) {
        fprintf(stderr, "%s\n", nids_errbuf);
        return 1;
    }

    nids_register_tcp(tcp_callback);

    /* HACK: nids checksum seems to be broken in some cases, so disable it.
       Might be related to http://bugs.debian.org/284937 */
    struct nids_chksum_ctl chksum_ctl;
    chksum_ctl.netaddr = 0;
    chksum_ctl.mask = 0;
    chksum_ctl.action = NIDS_DONT_CHKSUM;
    nids_register_chksum_ctl(&chksum_ctl, 1);

    fprintf(stderr, "Listening on %s\n", nids_params.device);
    nids_run();

    return 0;
}

