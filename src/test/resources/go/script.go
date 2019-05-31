package main

import (
	"fmt"
	"net/http"
	"time"
	"math/rand"
)

func main() {
	http.HandleFunc("/go", func (w http.ResponseWriter, r *http.Request) {
		fmt.Println("Request accepted at: " + time.Now().String())
		if rand.Intn(5) > 0 {
			fmt.Fprintf(w, "ok")
			fmt.Println("ok")
		} else {
			fmt.Fprintf(w, "error processing")
			fmt.Println("error processing")
		}
	})

	http.ListenAndServe(":9000", nil)
}
