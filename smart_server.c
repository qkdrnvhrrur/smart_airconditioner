#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <pthread.h>
#include <mqueue.h>

#define PORT 8090
#define MAX_CONN 5

typedef struct _Data{
    int status;
    int temp;
    int wind;
} Datas;

Datas datas={.status=0, .temp=24, .wind=0};
pthread_mutex_t mutex=PTHREAD_MUTEX_INITIALIZER;

void* do_control(void*);
void send_data(char* data, int len);

int conn_num=0;
int connPool[MAX_CONN];

int main(void){
    int connSock, listenSock;
    struct sockaddr_in s_addr, c_addr;
    int len;

    pthread_t pthread1;
    int thr_st;

    listenSock=socket(PF_INET, SOCK_STREAM, 0);

    memset(&s_addr, 0, sizeof(s_addr));
    s_addr.sin_addr.s_addr=htonl(INADDR_ANY);
    s_addr.sin_family=AF_INET;
    s_addr.sin_port=htons(PORT);

    if(bind(listenSock, (struct sockaddr*)&s_addr, sizeof(s_addr))==-1){
        printf("can not bind\n");
        return -1;
    }

    if(listen(listenSock, 5)==-1){
        printf("listen fail\n");
        return -1;
    }

    while(1){
        len=sizeof(c_addr);

        if(conn_num<5){
            if((connSock=accept(listenSock, (struct sockaddr*)&c_addr, &len))!=-1)
                printf("accept!!!\n");
        }
        else
            printf("max connection : %d\n", MAX_CONN);

        while(conn_num==5);

        pthread_mutex_lock(&mutex);
        connPool[conn_num++]=connSock;
        pthread_mutex_unlock(&mutex);
        printf("the number of connection : %d\n", conn_num);

        thr_st=pthread_create(&pthread1, NULL, do_control, &connSock);
        if(thr_st!=0)
            printf("thread create fail\n");

        pthread_detach(pthread1);
    }

    close(listenSock);

    return 0;
}

void* do_control(void* data){
    int n;
    char rcvBuffer[BUFSIZ], sndBuffer[BUFSIZ];
    int* temp=data;
    int connSock=*temp;

    sprintf(sndBuffer, "%d:%d:%d", datas.status, datas.temp, datas.wind);
    printf("%s\n", sndBuffer);
    if((n=write(connSock, sndBuffer, strlen(sndBuffer)))<0){
        exit(1);
    }

    while((n=read(connSock, rcvBuffer, sizeof(rcvBuffer)))!=0){
        if(!strcmp(rcvBuffer, "0000\n")){
            pthread_mutex_lock(&mutex);
            datas.status=1;
            pthread_mutex_unlock(&mutex);
            
            system("sudo irsend SEND_ONCE acmin KEY_POWER &");
            sprintf(sndBuffer, "stat:%d", datas.status);
            printf("%s\n", sndBuffer);

            send_data(sndBuffer, strlen(sndBuffer));
        }
        else if(!strcmp(rcvBuffer, "9999\n")){
            pthread_mutex_lock(&mutex);
            datas.status=0;
            pthread_mutex_unlock(&mutex);

            system("sudo irsend SEND_ONCE acmin KEY_POWER KEY_POWER &");

            sprintf(sndBuffer, "stat:%d", datas.status);
            printf("%s\n", sndBuffer);

            send_data(sndBuffer, strlen(sndBuffer));
        }
        else if((!strcmp(rcvBuffer, "1111\n"))&&(datas.status==1)){
            if(datas.temp<30){
                pthread_mutex_lock(&mutex);
                datas.temp++;
                pthread_mutex_unlock(&mutex);

                system("sudo irsend SEND_ONCE acmin KEY_UP &");
            }
            
            sprintf(sndBuffer, "temp:%d", datas.temp);
            printf("%s\n", sndBuffer);
            
            send_data(sndBuffer, strlen(sndBuffer));
        }
        else if((!strcmp(rcvBuffer, "2222\n"))&&(datas.status==1)){
            if(datas.temp>18){
                pthread_mutex_lock(&mutex);
                datas.temp--;
                pthread_mutex_unlock(&mutex);

                system("sudo irsend SEND_ONCE acmin KEY_DOWN &");
            }

            sprintf(sndBuffer, "temp:%d", datas.temp);
            printf("%s\n", sndBuffer);
            
            send_data(sndBuffer, strlen(sndBuffer));
        }
        else if((!strcmp(rcvBuffer, "3333\n"))&&(datas.status==1)){
            if(datas.wind<3){
                pthread_mutex_lock(&mutex);
                datas.wind++;
                pthread_mutex_unlock(&mutex);
            
                system("sudo irsend SEND_ONCE acmin KEY_RIGHT &");
            }
            else{
                pthread_mutex_lock(&mutex);
                datas.wind=1;
                pthread_mutex_unlock(&mutex);

                system("sudo irsend SEND_ONCE acmin KEY_RIGHT &");
            }
            
            sprintf(sndBuffer, "wind:%d", datas.wind);
            printf("%s\n", sndBuffer);

            send_data(sndBuffer, strlen(sndBuffer));
        }
        /*else if((!strcmp(rcvBuffer, "4444\n"))&&(datas.status==1)){
            if(datas.wind>1){
                pthread_mutex_lock(&mutex);
                datas.wind--;
                pthread_mutex_unlock(&mutex);
            
                system("sudo irsend SEND_ONCE acmin KEY_LEFT &");
            }

            sprintf(sndBuffer, "wind:%d", datas.wind);
            printf("%s\n", sndBuffer);
            
            send_data(sndBuffer, strlen(sndBuffer));
        }*/
    }

    // remove disconnected client
    pthread_mutex_lock(&mutex);
    for (int i=0; i<conn_num; i++)
    {
        if (connSock==connPool[i])
        {
            while(i++<conn_num-1)
                connPool[i]=connPool[i+1];
            break;
        }
    }
    conn_num--;
    pthread_mutex_unlock(&mutex);

    printf("EOF\n");
    close(connSock);
}

void send_data(char* data, int len){
    int i;

    pthread_mutex_lock(&mutex);
    for (i=0; i<conn_num; i++)
        write(connPool[i], data, len);
    pthread_mutex_unlock(&mutex);
}

